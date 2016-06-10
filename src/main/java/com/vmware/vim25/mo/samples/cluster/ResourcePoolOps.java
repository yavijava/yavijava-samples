package com.vmware.vim25.mo.samples.cluster;

import com.vmware.vim25.ResourceAllocationInfo;
import com.vmware.vim25.ResourcePoolSummary;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.samples.SampleUtil;

public class ResourcePoolOps {

   public static void main(String[] args) throws Exception {
      ServiceInstance si = SampleUtil.createServiceInstance();
      Folder rootFolder = si.getRootFolder();
      Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder).searchManagedEntities("Datacenter")[0];
      ManagedEntity[] entities = new InventoryNavigator(dc).searchManagedEntities("ResourcePool");
      System.out.println("============ Resource Pools ============");
      for (ManagedEntity entity : entities) {
         ResourcePool rp = (ResourcePool) entity;
         System.out.println("Name: " + rp.getName());
         System.out.println("-- Moid: " + rp.getMOR().getVal());
         ResourcePoolSummary sum = rp.getSummary();
         ResourceAllocationInfo info;
         info = sum.getConfig().getCpuAllocation();
         System.out.println("-- CPU reservation: " + info.getReservation());
         System.out.println("-- CPU limit: " + info.getLimit());
         info = sum.getConfig().getMemoryAllocation();
         System.out.println("-- Mem reservation: " + info.getReservation());
         System.out.println("-- Mem limit: " + info.getLimit());
      }
   }

}
