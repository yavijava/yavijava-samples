package com.vmware.vim25.mo.samples;

import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.mo.ServiceInstance;

public class VCenterServerInfo {

   public static void main(String[] args) throws Exception {
      ServiceInstance si = SampleUtil.createServiceInstance();

      AboutInfo info = si.getAboutInfo();
      System.out.println("=== vCenter Server Summary ===");
      System.out.println("Name: " + info.getFullName());
      System.out.println("UUID: " + info.getInstanceUuid());
      System.out.println("Vendor: " + info.getVendor());
      System.out.println("Version: " + info.getVersion());
      System.out.println("OS Type: " + info.getOsType());
      System.out.println("API Version: " + info.getApiVersion());
      System.out.println("Cookie: " + si.getServerConnection().getSessionStr());

      si.getServerConnection().logout();
   }

}
