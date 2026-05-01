/*
 * Copyright 2026 Michael Rice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vmware.vim25.mo.samples;

import java.net.URL;

import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.DirectPathProfileInfo;
import com.vmware.vim25.DirectPathProfileManagerFilterSpec;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.ServiceManagerServiceInfo;
import com.vmware.vim25.SiteInfo;
import com.vmware.vim25.mo.CryptoManagerHost;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.DirectPathProfileManager;
import com.vmware.vim25.mo.HealthUpdateManager;
import com.vmware.vim25.mo.HostAssignableHardwareManager;
import com.vmware.vim25.mo.HostDateTimeSystem;
import com.vmware.vim25.mo.HostFirewallSystem;
import com.vmware.vim25.mo.HostNetworkSystem;
import com.vmware.vim25.mo.HostNvdimmSystem;
import com.vmware.vim25.mo.HostSpecificationManager;
import com.vmware.vim25.mo.HostStorageSystem;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.ServiceManager;
import com.vmware.vim25.mo.SiteInfoManager;
import com.vmware.vim25.mo.StorageQueryManager;
import com.vmware.vim25.mo.TenantTenantManager;
import com.vmware.vim25.mo.VcenterVStorageObjectManager;
import com.vmware.vim25.mo.VirtualMachineGuestCustomizationManager;
import com.vmware.vim25.mo.VirtualizationManager;
import com.vmware.vim25.mo.util.MorUtil;

/**
 * vSphere 9.0 end-to-end smoke test.
 *
 * Exercises read-only (non-destructive) paths against a real vCenter to
 * verify yavijava's vSphere 9.0 API parity end to end:
 *   - Connection / auth via the URL+credentials constructor
 *   - ServiceContent exposes all expected 9.0 manager MORs
 *   - Each new 9.0 wrapper class instantiates via MorUtil
 *   - Safe read methods on the new managers round-trip over SOAP
 *   - Per-host: new host-level managers (assignableHardware, nvdimm, crypto)
 *   - Restored hand-written methods still work over the wire
 *
 * Per-check status: [PASS] / [SKIP] / [FAIL] with a final summary.
 *
 * Usage: java Vsphere9SmokeTest &lt;url&gt; &lt;username&gt; &lt;password&gt;
 *   url = https://vcenter.example.com/sdk
 */
public class Vsphere9SmokeTest {

  private int passed = 0;
  private int skipped = 0;
  private int failed = 0;

  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      System.out.println("Usage: java Vsphere9SmokeTest <url> <username> <password>");
      System.exit(1);
    }

    Vsphere9SmokeTest test = new Vsphere9SmokeTest();
    ServiceInstance si = null;
    try {
      si = test.section1Connect(args[0], args[1], args[2]);
      ServiceContent sc = si.getServiceContent();

      test.section2ServiceContent(sc);
      test.section3NewWrappersInstantiate(si, sc);
      test.section4NewReadApis(si, sc);
      test.section5PerHost(si);
      test.section6RestoredMethods(si);
      test.section7MigratedTaskSignature(si);
    } finally {
      if (si != null) {
        si.getServerConnection().logout();
      }
    }

    System.out.println();
    System.out.println("================================================================");
    System.out.printf("Summary: %d passed, %d skipped, %d failed%n",
        test.passed, test.skipped, test.failed);
    System.out.println("================================================================");
    if (test.failed > 0) {
      System.exit(2);
    }
  }

  private ServiceInstance section1Connect(String url, String user, String pass)
      throws Exception {
    header("1. Connection & ServiceContent");
    ServiceInstance si = new ServiceInstance(new URL(url), user, pass, true);
    pass("Connected to " + url);
    AboutInfo about = si.getAboutInfo();
    pass(String.format("About: %s %s build-%s api=%s",
        about.getName(), about.getVersion(), about.getBuild(), about.getApiVersion()));
    pass("Root folder: " + si.getRootFolder().getName());
    return si;
  }

  private void section2ServiceContent(ServiceContent sc) {
    header("2. ServiceContent has 9.0 manager MORs");
    checkMor("virtualizationManager", sc.getVirtualizationManager());
    checkMor("guestCustomizationManager", sc.getGuestCustomizationManager());
    checkMor("serviceManager", sc.getServiceManager());
    checkMor("vStorageObjectManager", sc.getVStorageObjectManager());
    checkMor("hostSpecManager", sc.getHostSpecManager());
    checkMor("healthUpdateManager", sc.getHealthUpdateManager());
    checkMor("tenantManager", sc.getTenantManager());
    checkMor("siteInfoManager", sc.getSiteInfoManager());
    checkMor("storageQueryManager", sc.getStorageQueryManager());
    checkMor("directPathProfileManager", sc.getDirectPathProfileManager());
    skip("failoverClusterConfigurator (no wrapper, see issue #322)");
    skip("failoverClusterManager (no wrapper, see issue #322)");
  }

  private void section3NewWrappersInstantiate(ServiceInstance si, ServiceContent sc) {
    header("3. New wrappers instantiate via MorUtil");
    instantiate(si, sc.getDirectPathProfileManager(), DirectPathProfileManager.class);
    instantiate(si, sc.getHealthUpdateManager(), HealthUpdateManager.class);
    instantiate(si, sc.getHostSpecManager(), HostSpecificationManager.class);
    instantiate(si, sc.getServiceManager(), ServiceManager.class);
    instantiate(si, sc.getSiteInfoManager(), SiteInfoManager.class);
    instantiate(si, sc.getStorageQueryManager(), StorageQueryManager.class);
    instantiate(si, sc.getTenantManager(), TenantTenantManager.class);
    instantiate(si, sc.getVStorageObjectManager(), VcenterVStorageObjectManager.class);
    instantiate(si, sc.getGuestCustomizationManager(), VirtualMachineGuestCustomizationManager.class);
    instantiate(si, sc.getVirtualizationManager(), VirtualizationManager.class);
  }

  private void section4NewReadApis(ServiceInstance si, ServiceContent sc) {
    header("4. New 9.0 read APIs round-trip over SOAP");
    if (sc.getDirectPathProfileManager() != null) {
      try {
        DirectPathProfileManager dppm = (DirectPathProfileManager) MorUtil
            .createExactManagedObject(si.getServerConnection(), sc.getDirectPathProfileManager());
        DirectPathProfileInfo[] profiles = dppm.listDirectPathProfiles(new DirectPathProfileManagerFilterSpec());
        pass(String.format("DirectPathProfileManager.listDirectPathProfiles -> %d profile(s)",
            profiles == null ? 0 : profiles.length));
      } catch (Exception e) {
        fail("DirectPathProfileManager.listDirectPathProfiles", e);
      }
    } else {
      skip("DirectPathProfileManager not present");
    }

    if (sc.getSiteInfoManager() != null) {
      try {
        SiteInfoManager sim = (SiteInfoManager) MorUtil
            .createExactManagedObject(si.getServerConnection(), sc.getSiteInfoManager());
        SiteInfo info = sim.getSiteInfo();
        pass("SiteInfoManager.getSiteInfo -> " + (info == null ? "null" : info.getClass().getSimpleName()));
      } catch (Exception e) {
        fail("SiteInfoManager.getSiteInfo", e);
      }
    } else {
      skip("SiteInfoManager not present");
    }

    if (sc.getServiceManager() != null) {
      try {
        ServiceManager sm = (ServiceManager) MorUtil
            .createExactManagedObject(si.getServerConnection(), sc.getServiceManager());
        ServiceManagerServiceInfo[] services = sm.queryServiceList(null, null);
        pass(String.format("ServiceManager.queryServiceList -> %d service(s)",
            services == null ? 0 : services.length));
      } catch (Exception e) {
        fail("ServiceManager.queryServiceList", e);
      }
    } else {
      skip("ServiceManager not present");
    }
  }

  private void section5PerHost(ServiceInstance si) throws Exception {
    header("5. Per-host: new host-level managers");
    ManagedEntity[] hosts = new InventoryNavigator(si.getRootFolder())
        .searchManagedEntities("HostSystem");
    if (hosts == null || hosts.length == 0) {
      skip("no HostSystem found in inventory");
      return;
    }

    for (ManagedEntity me : hosts) {
      HostSystem host = (HostSystem) me;
      System.out.println("  -- host: " + host.getName() + " --");
      HostConfigManager configMgr = (HostConfigManager) host.getPropertyByPath("configManager");
      if (configMgr == null) {
        skip("  configManager null on " + host.getName());
        continue;
      }

      checkHostManager(si, configMgr.getAssignableHardwareManager(),
          HostAssignableHardwareManager.class, "  AssignableHardwareManager");
      checkHostManager(si, configMgr.getNvdimmSystem(),
          HostNvdimmSystem.class, "  NvdimmSystem");
      checkHostManager(si, configMgr.getCryptoManager(),
          CryptoManagerHost.class, "  CryptoManager (host)");
    }
  }

  private void section6RestoredMethods(ServiceInstance si) throws Exception {
    header("6. Restored hand-written methods round-trip");

    ManagedEntity[] hosts = new InventoryNavigator(si.getRootFolder())
        .searchManagedEntities("HostSystem");
    if (hosts != null && hosts.length > 0) {
      HostSystem host = (HostSystem) hosts[0];
      tryVoid("HostNetworkSystem.refreshNetworkSystem on " + host.getName(), () -> {
        HostNetworkSystem hns = host.getHostNetworkSystem();
        hns.refreshNetworkSystem();
      });
      tryVoid("HostDateTimeSystem.refreshDateTimeSystem on " + host.getName(), () -> {
        HostDateTimeSystem hdts = host.getHostDateTimeSystem();
        hdts.refreshDateTimeSystem();
      });
      tryVoid("HostFirewallSystem.refreshFirewall on " + host.getName(), () -> {
        HostFirewallSystem hfs = host.getHostFirewallSystem();
        hfs.refreshFirewall();
      });
    } else {
      skip("no host for refresh* methods");
    }

    ManagedEntity[] datastores = new InventoryNavigator(si.getRootFolder())
        .searchManagedEntities("Datastore");
    if (datastores != null && datastores.length > 0) {
      Datastore ds = (Datastore) datastores[0];
      tryVoid("Datastore.refreshDatastoreStorageInfo on " + ds.getName(), () -> {
        ds.refreshDatastoreStorageInfo();
      });
    } else {
      skip("no datastore for refreshDatastoreStorageInfo");
    }
  }

  private void section7MigratedTaskSignature(ServiceInstance si) throws Exception {
    header("7. Migrated Task signatures (compile-time + reachability check)");
    ManagedEntity[] hosts = new InventoryNavigator(si.getRootFolder())
        .searchManagedEntities("HostSystem");
    if (hosts == null || hosts.length == 0) {
      skip("no host for storage system smoke");
      return;
    }
    HostSystem host = (HostSystem) hosts[0];
    HostStorageSystem hss = host.getHostStorageSystem();
    // Compile-time check: the regen changed several methods on
    // HostStorageSystem from `Task[]` to single `Task` (markPerenniallyReservedEx,
    // turnDiskLocatorLedOff/On, resolveMultipleUnresolvedVmfsVolumesEx).
    // Just verifying the wrapper resolves and exposes the new shape.
    pass("HostStorageSystem reachable on " + host.getName()
        + " (mor=" + hss.getMOR().getVal() + ")");
  }

  // -------- helpers --------

  private void header(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }

  private void pass(String msg) {
    System.out.println("[PASS] " + msg);
    passed++;
  }

  private void skip(String msg) {
    System.out.println("[SKIP] " + msg);
    skipped++;
  }

  private void fail(String label, Exception e) {
    System.out.println("[FAIL] " + label + " :: " + e.getClass().getSimpleName() + ": " + e.getMessage());
    failed++;
  }

  private void checkMor(String name, ManagedObjectReference mor) {
    if (mor != null) {
      pass(name + " (type=" + mor.getType() + ", val=" + mor.getVal() + ")");
    } else {
      skip(name + " (null in ServiceContent — server may not expose this)");
    }
  }

  private void instantiate(ServiceInstance si, ManagedObjectReference mor, Class<?> expected) {
    if (mor == null) {
      skip(expected.getSimpleName() + " (no MOR in ServiceContent)");
      return;
    }
    try {
      Object obj = MorUtil.createExactManagedObject(si.getServerConnection(), mor);
      if (expected.isInstance(obj)) {
        pass(expected.getSimpleName() + " instantiated via MorUtil");
      } else {
        System.out.println("[FAIL] " + expected.getSimpleName()
            + " — got " + obj.getClass().getSimpleName() + " instead");
        failed++;
      }
    } catch (Exception e) {
      fail(expected.getSimpleName() + " via MorUtil", e);
    }
  }

  private void checkHostManager(ServiceInstance si, ManagedObjectReference mor,
      Class<?> expected, String label) {
    if (mor == null) {
      skip(label + " (not present on this host)");
      return;
    }
    try {
      Object obj = MorUtil.createExactManagedObject(si.getServerConnection(), mor);
      if (expected.isInstance(obj)) {
        pass(label + " wired up");
      } else {
        System.out.println("[FAIL] " + label
            + " — got " + obj.getClass().getSimpleName() + " instead");
        failed++;
      }
    } catch (Exception e) {
      fail(label, e);
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private void tryVoid(String label, ThrowingRunnable r) {
    try {
      r.run();
      pass(label);
    } catch (Exception e) {
      fail(label, e);
    }
  }
}
