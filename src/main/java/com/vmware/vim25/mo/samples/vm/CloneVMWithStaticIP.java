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

import com.vmware.vim25.CustomizationAdapterMapping;
import com.vmware.vim25.CustomizationFixedIp;
import com.vmware.vim25.CustomizationGlobalIPSettings;
import com.vmware.vim25.CustomizationIPSettings;
import com.vmware.vim25.CustomizationSpec;
import com.vmware.vim25.CustomizationSpecItem;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.CustomizationSpecManager;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * Clones a VM from a template and applies a static IP address using a vCenter
 * OS Customization Spec as the base for identity/OS settings, then overrides
 * the NIC mapping with user-supplied static IP configuration.
 *
 * Based on community code from yavijava GitHub issue #248.
 *
 * Usage:
 *   java CloneVMWithStaticIP <url> <username> <password> <template-name>
 *       <clone-name> <customization-spec-name> <ip> <gateway> <subnet> <dns>
 *
 * http://vijava.sf.net
 */
public class CloneVMWithStaticIP
{
  public static void main(String[] args) throws Exception
  {
    if (args.length != 10)
    {
      System.out.println("Usage: java CloneVMWithStaticIP <url> <username> <password>" +
          " <template-name> <clone-name> <customization-spec-name>" +
          " <ip> <gateway> <subnet> <dns>");
      System.exit(0);
    }

    String url              = args[0];
    String username         = args[1];
    String password         = args[2];
    String templateName     = args[3];
    String cloneName        = args[4];
    String specName         = args[5];
    String ipAddress        = args[6];
    String gateway          = args[7];
    String netmask          = args[8];
    String dnsServer        = args[9];

    ServiceInstance si = new ServiceInstance(new URL(url), username, password, true);

    Folder rootFolder = si.getRootFolder();

    VirtualMachine template = (VirtualMachine) new InventoryNavigator(
        rootFolder).searchManagedEntity("VirtualMachine", templateName);

    if (template == null)
    {
      System.out.println("No VM/template named '" + templateName + "' found.");
      si.getServerConnection().logout();
      return;
    }

    // Load the named OS Customization Spec from vCenter to get identity/OS settings.
    CustomizationSpecManager csManager = si.getCustomizationSpecManager();
    CustomizationSpecItem cSpecItem = csManager.getCustomizationSpec(specName);

    if (cSpecItem == null)
    {
      System.out.println("No customization spec named '" + specName + "' found.");
      si.getServerConnection().logout();
      return;
    }

    // Build the customization spec, borrowing identity and options from the
    // named spec, then overriding the NIC mapping with static IP settings.
    CustomizationSpec vmCustomizationSpec = new CustomizationSpec();
    vmCustomizationSpec.setOptions(cSpecItem.getSpec().getOptions());
    vmCustomizationSpec.setIdentity(cSpecItem.getSpec().getIdentity());

    // Global IP settings (DNS servers and suffix list).
    CustomizationGlobalIPSettings vmIpSettings = new CustomizationGlobalIPSettings();
    vmIpSettings.setDnsServerList(new String[] { dnsServer });
    vmIpSettings.setDnsSuffixList(new String[] { "local" });

    // Fixed (static) IP for the first NIC.
    CustomizationFixedIp vmFixedIp = new CustomizationFixedIp();
    vmFixedIp.setIpAddress(ipAddress);

    CustomizationIPSettings vmAdapter = new CustomizationIPSettings();
    vmAdapter.setIp(vmFixedIp);
    vmAdapter.setGateway(new String[] { gateway });
    vmAdapter.setSubnetMask(netmask);

    CustomizationAdapterMapping adapterMap = new CustomizationAdapterMapping();
    adapterMap.setAdapter(vmAdapter);

    vmCustomizationSpec.setGlobalIPSettings(vmIpSettings);
    vmCustomizationSpec.setNicSettingMap(new CustomizationAdapterMapping[] { adapterMap });

    // Build the clone spec.
    VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
    cloneSpec.setLocation(new VirtualMachineRelocateSpec());
    cloneSpec.setPowerOn(false);
    cloneSpec.setTemplate(false);
    cloneSpec.setCustomization(vmCustomizationSpec);

    System.out.println("Launching the VM clone task. Please wait ...");

    Task task = template.cloneVM_Task((Folder) template.getParent(), cloneName, cloneSpec);

    String status = task.waitForMe();
    if (status == Task.SUCCESS)
    {
      System.out.println("VM cloned successfully as '" + cloneName + "' with static IP " + ipAddress + ".");
    }
    else
    {
      System.out.println("Failure: VM could not be cloned.");
    }

    si.getServerConnection().logout();
  }
}
