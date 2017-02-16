/*================================================================================
Copyright (c) 2008 VMware, Inc. All Rights Reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, 
this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice, 
this list of conditions and the following disclaimer in the documentation 
and/or other materials provided with the distribution.

 * Neither the name of VMware, Inc. nor the names of its contributors may be used
to endorse or promote products derived from this software without specific prior 
written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL VMWARE, INC. OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.
================================================================================*/
package com.vmware.vim25.mo.samples.vm;

import java.net.URL;

import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.EnvironmentBrowser;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * http://vijava.sf.net
 * @author Eric Cornely <ericcornely@gmail.com>
 */

public class VmNicChangeBacking 
{    
  public static void main(String[] args) throws Exception 
  {     
    if(args.length!=6)
    {
      System.out.println("Usage: java VmNicChangeBacking <url> <username> <password> <vmname> <mac> <dvPort>");
      System.exit(0);
    }
    String url = args[0];
    String username = args[1];
    String password = args[2];
    String vmname = args[3];
    String mac = args[4];
    String dvPort = args[5];
 
    ServiceInstance serviceInstance = new ServiceInstance(new URL(url), username, password, true);

    Folder rootFolder = serviceInstance.getRootFolder();
    VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);
    if(vm==null)
    {
      System.out.println("No VM " + vmname + " found");
      serviceInstance.getServerConnection().logout();
      return;
    }
    
    DistributedVirtualPortgroupInfo dvPortgroupInfo = getDvPortGroupInfo(vm, dvPort);
    /*Create a distributed backing with the found dvPort*/
    VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
    nicBacking.port = new DistributedVirtualSwitchPortConnection();
    nicBacking.port.portgroupKey = dvPortgroupInfo.portgroupKey;
    nicBacking.port.switchUuid = dvPortgroupInfo.switchUuid;

    /*Searching the nic with the mac*/
    VirtualDeviceConfigSpec configSpec = createDeviceConfig(vm, mac, nicBacking);
    
    if (configSpec != null) {
      VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
      
      vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec[] { configSpec });
      Task task = vm.reconfigVM_Task(vmConfigSpec);
      String result = task.waitForTask();
      if (!Task.SUCCESS.equals(result)) {
        throw new Exception("Impossible to change vlan");
      }
    } else {
      throw new Exception("Cannot find network card with " + mac + " on vm " + vm.getName());
    }
    serviceInstance.getServerConnection().logout();
  }

  private static VirtualDeviceConfigSpec createDeviceConfig(VirtualMachine vm, String mac, VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking) {
    VirtualDeviceConfigSpec configSpec = null;
    VirtualMachineConfigInfo vmConfigInfo = vm.getConfig();
    VirtualDevice[] vds = vmConfigInfo.getHardware().getDevice();
    int i = 0;
    while (configSpec == null && i < vds.length) {
      if (vds[i] instanceof VirtualEthernetCard && ((VirtualEthernetCard) vds[i]).getMacAddress().equals(mac)) {
        configSpec = new VirtualDeviceConfigSpec();
        configSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
        vds[i].setBacking(nicBacking);
        configSpec.setDevice(vds[i]);
      }
      i++;
    }
    return configSpec;
  }
  
  /*Search a dvPortGroup by name in a the vm HostSystem */
  private static DistributedVirtualPortgroupInfo getDvPortGroupInfo(VirtualMachine vm, String dvPort) throws Exception{
    HostSystem host = new HostSystem(vm.getServerConnection(), vm.getRuntime().getHost());
    ComputeResource cr = (ComputeResource) host.getParent();
    EnvironmentBrowser envBrowser = cr.getEnvironmentBrowser();
    ConfigTarget configTarget = envBrowser.queryConfigTarget(host);
    
    DistributedVirtualPortgroupInfo dvPortgroupInfo = null;
    DistributedVirtualPortgroupInfo[] availableDvPortGroupInfo = configTarget.getDistributedVirtualPortgroup();
    int j = 0;
    while (j < availableDvPortGroupInfo.length && dvPortgroupInfo == null) {
      dvPortgroupInfo = (availableDvPortGroupInfo[j].portgroupName.equalsIgnoreCase(dvPort)) ? availableDvPortGroupInfo[j] : null;
      j++;
    }
    if (dvPortgroupInfo == null) {
      throw new Exception("Impossible to find dvPortGroupInfo with name : " + dvPort);
    }
    return dvPortgroupInfo;
  }
 
}