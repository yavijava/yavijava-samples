package com.vmware.vim25.mo.samples.storage;

import com.vmware.vim25.PodStorageDrsEntry;
import com.vmware.vim25.StorageDrsConfigInfo;
import com.vmware.vim25.StorageDrsIoLoadBalanceConfig;
import com.vmware.vim25.StorageDrsPodConfigInfo;
import com.vmware.vim25.StorageDrsSpaceLoadBalanceConfig;
import com.vmware.vim25.StoragePodSummary;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.StoragePod;
import com.vmware.vim25.mo.samples.SampleUtil;

public class GetStorageDRSConfig {

   public static void main(String[] args) throws Exception {
      ServiceInstance si = SampleUtil.createServiceInstance();
      Folder rootFolder = si.getRootFolder();
      Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder).searchManagedEntities("Datacenter")[0];
      ManagedEntity[] entities = new InventoryNavigator(dc).searchManagedEntities("StoragePod");
      for (ManagedEntity entity : entities) {
         StoragePod sp = (StoragePod) entity;
         StoragePodSummary pss = sp.getSummary();
         System.out.println("=============== StoragePod Summary ===============");
         System.out.println("Name: " + pss.getName());
         System.out.println("Capacity: " + pss.getCapacity());
         System.out.println("FreeSpace: " + pss.getFreeSpace());

         PodStorageDrsEntry psde = sp.getPodStorageDrsEntry();
         StorageDrsConfigInfo sdc = psde.getStorageDrsConfig();
         System.out.println("=============== Storage DRS Configurations ===============");
         StorageDrsPodConfigInfo sdpc = sdc.getPodConfig();
         System.out.println("Enabled: " + sdpc.isEnabled());
         System.out.println("LoadBalanceInterval: " + sdpc.getLoadBalanceInterval());
         System.out.println("DefaultVmBehavior: " + sdpc.getDefaultVmBehavior());

         StorageDrsSpaceLoadBalanceConfig sdslbc = sdpc.getSpaceLoadBalanceConfig();
         System.out.println("SpaceThresholdMode: " + sdslbc.getSpaceThresholdMode());
         System.out.println("FreeSpaceThresholdGB: " + sdslbc.getFreeSpaceThresholdGB());

         StorageDrsIoLoadBalanceConfig sdilbc = sdpc.getIoLoadBalanceConfig();
         System.out.println("IoLatencyThreshold: " + sdilbc.getIoLatencyThreshold());
         System.out.println("IoLoadImbalanceThreshold: " + sdilbc.getIoLoadImbalanceThreshold());
      }
   }
}
