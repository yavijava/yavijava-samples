package com.vmware.vim25.mo.samples.guestos;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.SampleUtil;

public class ChangeGuestOSPassword {

   public static void main(String[] args) throws RemoteException, MalformedURLException, InterruptedException {
      ServiceInstance si = SampleUtil.createServiceInstance();
      Folder rootFolder = si.getRootFolder();

      String vmName = "test-vm";
      VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine",
            vmName);
      if (vm == null) {
         System.out.println("Can not find VM " + vmName);
         return;
      }

      GuestOperationsManager gom = si.getGuestOperationsManager();

      if (!"guestToolsRunning".equals(vm.getGuest().toolsRunningStatus)) {
         System.out.println("The VMware Tools is not running in the Guest OS on VM " + vm.getName());
         System.out.println("Exiting...");
         return;
      }

      String osFamily = vm.getGuest().getGuestFamily();
      System.out.println("osFamily: " + osFamily);
      boolean isWindows = osFamily.contains("windowsGuest");
      String currentPassword = "password";
      String newPassword = "password";
      NamePasswordAuthentication creds = new NamePasswordAuthentication();
      creds.username = isWindows ? "Administrator" : "root";
      creds.password = currentPassword;

      GuestProgramSpec spec = new GuestProgramSpec();
      if (isWindows) {
         spec.programPath = "C:\\Windows\\System32\\cmd.exe";
         spec.arguments = "/C net user Administrator " + newPassword;
      } else {
         spec.programPath = "/bin/bash";
         spec.arguments = "-c 'echo " + newPassword + " | passwd root --stdin'";
      }
      GuestProcessManager gpm = gom.getProcessManager(vm);
      long pid = -1;
      try {
         pid = gpm.startProgramInGuest(creds, spec);
         System.out.println("pid: " + pid);
         Thread.sleep(3000);
         creds.password = newPassword;
         while (true) {
            GuestProcessInfo[] infoList = gpm.listProcessesInGuest(creds, new long[] { pid });
            GuestProcessInfo info = infoList[0];
            if (info.getExitCode() == null) {
               System.out.println("Waiting for the process to exit ... ");
               Thread.sleep(3000);
            } else {
               System.out.println("exit code: " + info.getExitCode());
               break;
            }
         }
      } catch (RemoteException e) {
         e.printStackTrace();
      }

      si.getServerConnection().logout();
   }
}
