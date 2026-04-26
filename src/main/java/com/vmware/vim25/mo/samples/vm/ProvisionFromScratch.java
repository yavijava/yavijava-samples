package com.vmware.vim25.mo.samples.vm;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

import java.net.URL;
import java.rmi.RemoteException;

/**
 * End-to-end provisioning sample for DRP (Digital Rebar Provisioner) discovery.
 *
 * Creates a datacenter, adds a standalone ESXi host, then provisions a
 * PXE-booting VM so DRP can discover and provision it on first power-on.
 *
 * Usage:
 *   java ProvisionFromScratch <vcenter-url> <vc-username> <vc-password>
 *                             <dc-name> <host-username> <host-password>
 *
 * Lab constants (edit these for your environment):
 */
public class ProvisionFromScratch {

    // -------------------------------------------------------------------------
    // Lab constants — customize for your environment
    // -------------------------------------------------------------------------
    static final String HOST_IP      = "192.168.1.213";
    static final String DATASTORE    = "bigdog";
    static final String NETWORK      = "VM Network";
    static final String VM_NAME      = "drp-discovery-vm";
    static final String GUEST_OS_ID  = "ubuntu64Guest";
    static final long   MEMORY_MB    = 4096L;
    static final int    CPU_COUNT    = 1;
    static final long   DISK_SIZE_KB = 20L * 1024 * 1024; // 20 GB
    // NIC device key — must match the key set on the NIC device below so that
    // VirtualMachineBootOptions can reference it for PXE boot ordering.
    static final int    NIC_KEY      = 4000;
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.out.println("Usage: java ProvisionFromScratch <vcenter-url> <vc-username> <vc-password>" +
                               " <dc-name> <host-username> <host-password>");
            System.exit(1);
        }

        String vcUrl      = args[0];
        String vcUser     = args[1];
        String vcPass     = args[2];
        String dcName     = args[3];
        String hostUser   = args[4];
        String hostPass   = args[5];

        // Step 1: Connect
        System.out.println("Connecting to vCenter: " + vcUrl);
        ServiceInstance si = new ServiceInstance(new URL(vcUrl), vcUser, vcPass, true);
        System.out.println("Connected. API version: " + si.getAboutInfo().getApiVersion());

        // Step 2: Create datacenter
        System.out.println("Creating datacenter: " + dcName);
        Folder rootFolder = si.getRootFolder();
        Datacenter dc = rootFolder.createDatacenter(dcName);
        System.out.println("Datacenter created: " + dc.getName());

        // Step 3: Add standalone host
        System.out.println("Adding host: " + HOST_IP);
        HostConnectSpec hostSpec = new HostConnectSpec();
        hostSpec.setHostName(HOST_IP);
        hostSpec.setUserName(hostUser);
        hostSpec.setPassword(hostPass);
        hostSpec.setForce(true);

        Task addHostTask = dc.getHostFolder()
                .addStandaloneHost_Task(hostSpec, null, true);
        waitForTask(addHostTask, "Add host " + HOST_IP);

        // Step 4: Find the host and its resource pool
        System.out.println("Locating host and resource pool in inventory...");
        InventoryNavigator inv = new InventoryNavigator(dc);
        HostSystem host = (HostSystem) inv.searchManagedEntity("HostSystem", HOST_IP);
        if (host == null) {
            System.err.println("ERROR: Could not find HostSystem '" + HOST_IP + "' after adding it.");
            System.exit(1);
        }
        ResourcePool rp = (ResourcePool) inv.searchManagedEntities("ResourcePool")[0];
        System.out.println("Using resource pool: " + rp.getName());

        // Step 5: Build VM config spec
        System.out.println("Building VM config spec for: " + VM_NAME);
        VirtualMachineConfigSpec vmSpec = buildVmSpec();

        // Step 6: Create VM
        System.out.println("Creating VM in datacenter VM folder...");
        Folder vmFolder = dc.getVmFolder();
        Task createTask = vmFolder.createVM_Task(vmSpec, rp, host);
        waitForTask(createTask, "Create VM " + VM_NAME);

        System.out.println("Done. VM '" + VM_NAME + "' created successfully.");
        System.out.println("Power it on to trigger PXE boot and DRP discovery.");

        si.getServerConnection().logout();
    }

    // -------------------------------------------------------------------------
    // VM spec construction
    // -------------------------------------------------------------------------

    private static VirtualMachineConfigSpec buildVmSpec() {
        VirtualMachineConfigSpec vmSpec = new VirtualMachineConfigSpec();
        vmSpec.setName(VM_NAME);
        vmSpec.setGuestId(GUEST_OS_ID);
        vmSpec.setMemoryMB(MEMORY_MB);
        vmSpec.setNumCPUs(CPU_COUNT);

        // vmx file location
        VirtualMachineFileInfo fileInfo = new VirtualMachineFileInfo();
        fileInfo.setVmPathName("[" + DATASTORE + "]");
        vmSpec.setFiles(fileInfo);

        vmSpec.setDeviceChange(new VirtualDeviceConfigSpec[]{
            buildScsiController(),
            buildDisk(),
            buildNic()
        });

        // PXE boot: boot from NIC first so DRP can discover the VM on power-on
        VirtualMachineBootOptions bootOptions = new VirtualMachineBootOptions();
        VirtualMachineBootOptionsBootableEthernetDevice netBoot =
            new VirtualMachineBootOptionsBootableEthernetDevice();
        netBoot.setDeviceKey(NIC_KEY);
        bootOptions.setBootOrder(new VirtualMachineBootOptionsBootableDevice[]{netBoot});
        vmSpec.setBootOptions(bootOptions);

        return vmSpec;
    }

    private static VirtualDeviceConfigSpec buildScsiController() {
        VirtualLsiLogicController scsi = new VirtualLsiLogicController();
        scsi.setKey(1000);
        scsi.setBusNumber(0);
        scsi.setSharedBus(VirtualSCSISharing.noSharing);

        VirtualDeviceConfigSpec spec = new VirtualDeviceConfigSpec();
        spec.setOperation(VirtualDeviceConfigSpecOperation.add);
        spec.setDevice(scsi);
        return spec;
    }

    private static VirtualDeviceConfigSpec buildDisk() {
        VirtualDiskFlatVer2BackingInfo backing = new VirtualDiskFlatVer2BackingInfo();
        backing.setFileName("[" + DATASTORE + "]");
        backing.setDiskMode("persistent");
        backing.setThinProvisioned(true);

        VirtualDisk disk = new VirtualDisk();
        disk.setKey(2000);
        disk.setControllerKey(1000);
        disk.setUnitNumber(0);
        disk.setCapacityInKB(DISK_SIZE_KB);
        disk.setBacking(backing);

        VirtualDeviceConfigSpec spec = new VirtualDeviceConfigSpec();
        spec.setOperation(VirtualDeviceConfigSpecOperation.add);
        spec.setFileOperation(VirtualDeviceConfigSpecFileOperation.create);
        spec.setDevice(disk);
        return spec;
    }

    private static VirtualDeviceConfigSpec buildNic() {
        VirtualEthernetCardNetworkBackingInfo backing =
            new VirtualEthernetCardNetworkBackingInfo();
        backing.setDeviceName(NETWORK);

        // vmxnet3 is required for PXE; PCNet32 does not support PXE boot
        VirtualVmxnet3 nic = new VirtualVmxnet3();
        nic.setKey(NIC_KEY);
        nic.setAddressType("generated");
        nic.setBacking(backing);

        Description info = new Description();
        info.setLabel("Network Adapter 1");
        info.setSummary(NETWORK);
        nic.setDeviceInfo(info);

        VirtualDeviceConfigSpec spec = new VirtualDeviceConfigSpec();
        spec.setOperation(VirtualDeviceConfigSpecOperation.add);
        spec.setDevice(nic);
        return spec;
    }

    // -------------------------------------------------------------------------
    // Task helper
    // -------------------------------------------------------------------------

    private static void waitForTask(Task task, String description)
            throws RemoteException, InterruptedException {
        System.out.println("  Waiting for task: " + description + "...");
        String result = task.waitForMe();
        if (!Task.SUCCESS.equals(result)) {
            System.err.println("ERROR: Task failed: " + description);
            System.err.println("  Result: " + result);
            System.exit(1);
        }
        System.out.println("  Task succeeded: " + description);
    }
}
