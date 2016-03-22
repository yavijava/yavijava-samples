package com.vmware.vim25.mo.samples.vm;

import java.rmi.RemoteException;

import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.SampleUtil;

public class VmConvert {

    public static void main(String[] args) throws Exception {
	ServiceInstance si = SampleUtil.createServiceInstance();

	VmConvert vmConvert = new VmConvert();
	vmConvert.convertToTemplate(si, "vm1");
	vmConvert.convertToVm(si, "vm1", "rp1", null);

	si.getServerConnection().logout();
    }

    public void convertToTemplate(ServiceInstance si, String vmName) throws Exception {
	VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("VirtualMachine", vmName);
	if (vm == null) {
	    System.out.println("Not found vm " + vmName);
	    return;
	}
	if (vm.getConfig().isTemplate()) {
	    System.out.println("The vm " + vmName + " is already a template!");
	    return;
	}
	vm.markAsTemplate();
	System.out.println("Convert vm " + vmName + " to template successfully!");
    }

    /**
     * @param hostName The target host on which the virtual machine is intended to run. If it is set to null, the default host will be used.
     */
    public void convertToVm(ServiceInstance si, String vmName, String poolName, String hostName) throws RemoteException {
	VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("VirtualMachine", vmName);
	if (vm == null) {
	    System.out.println("Not found vm " + vmName);
	    return;
	}
	ResourcePool pool = (ResourcePool) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("ResourcePool", poolName);
	if (pool == null) {
	    System.out.println("Not found resource pool " + poolName);
	    return;
	}
	HostSystem host = null;
	if (hostName != null) {
	    host = (HostSystem) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("HostSystem", hostName);
	    if (host == null) {
		System.out.println("Not found host " + hostName);
		return;
	    }
	}
	vm.markAsVirtualMachine(pool, host);
	System.out.println("Convert vm " + vmName + " from template successfully!");
    }

}
