package com.vmware.vim25.mo.samples.vm;

import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.SampleUtil;
import com.vmware.vim25.mox.VirtualMachineDeviceManager;

public class VmAddCdDriveFromIso {

   public static void main(String[] args) throws Exception {
      ServiceInstance si = SampleUtil.createServiceInstance();

      String vmname = "test_vm";
      Folder rootFolder = si.getRootFolder();
      VirtualMachine vm =
            (VirtualMachine) new InventoryNavigator(rootFolder)
                  .searchManagedEntity("VirtualMachine", vmname);

      if (vm == null) {
         System.out.println("No VM " + vmname + " found");
         si.getServerConnection().logout();
         return;
      }

      // method to mount a datastore iso to cd drive
      VirtualMachineDeviceManager vmdm = new VirtualMachineDeviceManager(vm);
      String isoPath = "[Datastore-154-2] iso/ubuntu-12.04.4-desktop-amd64.iso";
      boolean startConnected = true;
      Task task = vmdm.addCdDriveFromIso(isoPath, startConnected);

      String result = task.waitForMe();
      if (result == Task.SUCCESS) {
         System.out.println("ISO is mounted to cd drive successfully.");
      } else {
         System.out.println("Failed to mount the ISO to cd drive.");
         si.getServerConnection().logout();
         System.exit(1);
      }

      // method to remove the mounted cd drive
      VirtualDevice cdrom = vmdm.getDeviceByBackingFileName(isoPath);
      vmdm.removeDevice(cdrom, false);

      result = task.waitForMe();
      if (result == Task.SUCCESS) {
         System.out.println("The mounted cd drive is removed successfully.");
      } else {
         System.out.println("Failed to remove the mounted cd drive.");
      }

      si.getServerConnection().logout();
   }

}
