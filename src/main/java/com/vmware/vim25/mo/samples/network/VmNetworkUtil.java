package com.vmware.vim25.mo.samples.network;

import java.util.List;

import com.vmware.vim25.OptionValue;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.SampleUtil;
import com.vmware.vim25.mox.VirtualMachineDeviceManager;
import com.vmware.vim25.mox.VirtualMachineDeviceManager.VirtualNetworkAdapterType;

public class VmNetworkUtil {

    public static void main(String[] args) throws Exception {
	ServiceInstance si = SampleUtil.createServiceInstance();

	String vmName = "vm1";
	VmNetworkUtil vmNetUtil = new VmNetworkUtil();

	/*
	//add network adapter
	VirtualNetworkAdapterType type = VirtualNetworkAdapterType.VirtualVmxnet3;
	String networkName = "network adapter 2";
	String macAddress = null; //set to null for generated type
	boolean wakeOnLan = false;
	boolean startConnected = true;
	vmNetUtil.addNetworkAdapter(si, vmName, type, networkName, macAddress, wakeOnLan, startConnected);
	*/

	//list network adapter
	//vmNetUtil.listNetworkAdapter(si, vmName);

	//remove network adapter
	//vmNetUtil.removeNetworkAdapter(si, vmName, "Network adapter 2");

	//config static ip
	String ip = "10.111.111.101";
	String netmask = "255.255.255.0";
	String gateway = "10.111.111.1";
	String dns1 = "10.111.112.1";
	String dns2 = "10.111.112.2";
	vmNetUtil.configStaticIp(si, vmName, ip, netmask, gateway, dns1, dns2);

	si.getServerConnection().logout();
    }

    public void addNetworkAdapter(ServiceInstance si, String vmName, VirtualNetworkAdapterType type, String networkName, String macAddress, boolean wakeOnLan, boolean startConnected) throws Exception {
	VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("VirtualMachine", vmName);
	if (vm == null) {
	    System.out.println("Not found vm:" + vmName);
	    return;
	}
	VirtualMachineDeviceManager vmdm = new VirtualMachineDeviceManager(vm);
	vmdm.createNetworkAdapter(type, networkName, macAddress, wakeOnLan, startConnected);
	System.out.println("Add network:" + networkName + " for vm:" + vmName + " sucessfully!");
    }

    public void removeNetworkAdapter(ServiceInstance si, String vmName, String nicLable) throws Exception {
	VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("VirtualMachine", vmName);
	if (vm == null) {
	    System.out.println("Not found vm:" + vmName);
	    return;
	}
	VirtualMachineDeviceManager vmdm = new VirtualMachineDeviceManager(vm);
	List<VirtualEthernetCard> vics = vmdm.getVirtualDevicesOfType(VirtualEthernetCard.class);
	for (VirtualEthernetCard vic : vics) {
	    if (vic.getDeviceInfo().getLabel().equals(nicLable)) {
		Task task = vmdm.removeDevice(vic, false);
		String result = task.waitForTask();
		if (Task.SUCCESS.equals(result)) {
		    System.out.println("Remove NIC:" + nicLable + " for vm:" + vmName + " successfully!");
		} else {
		    throw new Exception("Remove NIC failed: result=" + result);
		}
		break;
	    }
	}
    }

    public void listNetworkAdapter(ServiceInstance si, String vmName) throws Exception {
	VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("VirtualMachine", vmName);
	if (vm == null) {
	    System.out.println("Not found vm:" + vmName);
	    return;
	}
	System.out.println("List NIC:");
	VirtualMachineDeviceManager vmdm = new VirtualMachineDeviceManager(vm);
	List<VirtualEthernetCard> vics = vmdm.getVirtualDevicesOfType(VirtualEthernetCard.class);
	for (VirtualEthernetCard vic : vics) {
	    System.out.println(vic.getDeviceInfo().getLabel());
	}
    }

    public void configStaticIp(ServiceInstance si, String vmName, String ip, String netmask, String gateway, String dns1, String dns2) throws Exception {
	VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("VirtualMachine", vmName);
	if (vm == null) {
	    System.out.println("Not found vm:" + vmName);
	    return;
	}
	VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
	OptionValue[] extraConfig = new OptionValue[1];
	extraConfig[0] = new OptionValue();
	//Use machine.id to store the ip related information. This information will be written to the .vmx file of the virtual machine.
	//And it can be retrieved with vmware tools inside the virtual machine, so the virtual machine can set up the ip, netmask, geteway, dns, etc. according to the information.
	//There are two sample scripts to retrive the information and set up the ip in the virtual machine, one for Linux(CentOS 6) and Windows(win 7):
	//Linux: src/main/resources/setup-ip.sh
	//Windows: src/main/resources/setup-ip.bat
	//More introduction about the machine.id can be found from:
	//http://tech.lazyllama.com/2010/06/22/passing-info-from-powercli-into-your-vm-using-guestinfo-variables/
	//https://communities.vmware.com/thread/443727?start=0&tstart=0
	extraConfig[0].setKey("machine.id");
	//The format of ip_config is: ip:ip_value,netmask:netmask_value,gateway:gateway_value,dns1:dns1_value,dns2:dns2_value
	extraConfig[0].setValue("ip:" + ip + ",netmask:" + netmask + ",gateway:" + gateway + ",dns1:" + dns1 + ",dns2:" + dns2);
	vmConfigSpec.setExtraConfig(extraConfig);
	Task task = vm.reconfigVM_Task(vmConfigSpec);
	String result = task.waitForTask();
	if (Task.SUCCESS.equals(result)) {
	    System.out.println("Configure static IP for vm:" + vmName + " successfully!");
	} else {
	    throw new Exception("Configure static IP failed: result=" + result);
	}
    }

}
