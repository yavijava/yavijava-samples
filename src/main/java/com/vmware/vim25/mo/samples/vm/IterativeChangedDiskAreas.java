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

package com.vmware.vim25.mo.samples.vm;

import java.net.URL;

import com.vmware.vim25.DiskChangeExtent;
import com.vmware.vim25.DiskChangeInfo;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;

/**
 * Demonstrates the iterative pattern for QueryChangedDiskAreas (CBT).
 *
 * vSphere caps the per-call result span well under a full disk (typically
 * around 2 TB minus a small remainder), so a single call against a large
 * (e.g. 2 TB+) disk only describes a slice of the disk and silently leaves
 * the rest unreported. To get every changed extent for the whole disk you
 * must loop: each call returns a DiskChangeInfo whose startOffset+length
 * tells you how far you've covered, and the next call uses that sum as
 * its startOffset. Repeat until the running offset reaches the disk's
 * capacity.
 *
 * Usage: java IterativeChangedDiskAreas &lt;url&gt; &lt;user&gt; &lt;password&gt;
 *                                       &lt;vmname&gt; &lt;diskLabel&gt; &lt;changeId&gt;
 *                                       [snapshotName]
 *
 *   diskLabel    - VirtualDisk device label (e.g. "Hard disk 1") or "*"
 *                  to use the first VirtualDisk on the VM.
 *   changeId     - The reference change ID from a prior backup; pass "*"
 *                  for a full-disk baseline.
 *   snapshotName - Optional. When omitted, queries against the VM's
 *                  current snapshot. CBT requires querying through a
 *                  snapshot.
 */
public class IterativeChangedDiskAreas {

  public static void main(String[] args) throws Exception {
    if (args.length < 6 || args.length > 7) {
      System.out.println("Usage: java IterativeChangedDiskAreas <url> <user> <password> "
          + "<vmname> <diskLabel|*> <changeId> [snapshotName]");
      System.exit(1);
    }

    String url = args[0];
    String user = args[1];
    String password = args[2];
    String vmname = args[3];
    String diskLabel = args[4];
    String changeId = args[5];
    String snapshotName = args.length == 7 ? args[6] : null;

    ServiceInstance si = new ServiceInstance(new URL(url), user, password, true);
    try {
      Folder rootFolder = si.getRootFolder();
      VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder)
          .searchManagedEntity("VirtualMachine", vmname);
      if (vm == null) {
        System.out.println("VM not found: " + vmname);
        return;
      }

      VirtualDisk disk = findDisk(vm, diskLabel);
      if (disk == null) {
        System.out.println("Disk not found on VM " + vmname + ": " + diskLabel);
        return;
      }

      long diskCapacityBytes = disk.getCapacityInBytes() != null
          ? disk.getCapacityInBytes()
          : disk.getCapacityInKB() * 1024L;

      VirtualMachineSnapshot snap = resolveSnapshot(vm, snapshotName);
      if (snap == null) {
        System.out.println("No snapshot available; CBT requires a snapshot context.");
        return;
      }

      System.out.printf("Disk \"%s\" (deviceKey=%d) capacity=%d bytes (~%.2f GiB)%n",
          disk.getDeviceInfo() != null ? disk.getDeviceInfo().getLabel() : "<unknown>",
          disk.getKey(),
          diskCapacityBytes,
          diskCapacityBytes / (1024.0 * 1024.0 * 1024.0));
      System.out.println("Walking QueryChangedDiskAreas in segments...");

      int calls = 0;
      int totalExtents = 0;
      long totalChangedBytes = 0;
      long offset = 0;
      while (offset < diskCapacityBytes) {
        DiskChangeInfo dci = vm.queryChangedDiskAreas(snap, disk.getKey(), offset, changeId);
        calls++;
        DiskChangeExtent[] extents = dci.getChangedArea();
        int extentCount = extents == null ? 0 : extents.length;
        totalExtents += extentCount;
        if (extents != null) {
          for (DiskChangeExtent e : extents) {
            totalChangedBytes += e.getLength();
          }
        }

        System.out.printf("  call=%d startOffset=%d length=%d extents=%d%n",
            calls, dci.getStartOffset(), dci.getLength(), extentCount);

        long advanced = dci.getStartOffset() + dci.getLength();
        if (advanced <= offset) {
          // Defensive: protect against a server that returns length=0 — without
          // this guard a buggy server reply would spin forever.
          System.out.println("  server did not advance offset; stopping.");
          break;
        }
        offset = advanced;
      }

      System.out.println();
      System.out.printf("Done. calls=%d coveredBytes=%d/%d extents=%d changedBytes=%d (~%.2f MiB)%n",
          calls, offset, diskCapacityBytes, totalExtents, totalChangedBytes,
          totalChangedBytes / (1024.0 * 1024.0));
    } finally {
      si.getServerConnection().logout();
    }
  }

  private static VirtualDisk findDisk(VirtualMachine vm, String diskLabel) {
    VirtualDevice[] devices = vm.getConfig().getHardware().getDevice();
    if (devices == null) {
      return null;
    }
    boolean any = "*".equals(diskLabel);
    for (VirtualDevice d : devices) {
      if (!(d instanceof VirtualDisk)) {
        continue;
      }
      if (any) {
        return (VirtualDisk) d;
      }
      String label = d.getDeviceInfo() != null ? d.getDeviceInfo().getLabel() : null;
      if (diskLabel.equals(label)) {
        return (VirtualDisk) d;
      }
    }
    return null;
  }

  private static VirtualMachineSnapshot resolveSnapshot(VirtualMachine vm, String snapshotName) {
    if (snapshotName == null) {
      ManagedObjectReference current = vm.getSnapshot() != null
          ? vm.getSnapshot().getCurrentSnapshot()
          : null;
      return current != null
          ? new VirtualMachineSnapshot(vm.getServerConnection(), current)
          : null;
    }
    if (vm.getSnapshot() == null) {
      return null;
    }
    return findInTree(vm, vm.getSnapshot().getRootSnapshotList(), snapshotName);
  }

  private static VirtualMachineSnapshot findInTree(VirtualMachine vm,
      VirtualMachineSnapshotTree[] trees, String name) {
    if (trees == null) {
      return null;
    }
    for (VirtualMachineSnapshotTree t : trees) {
      if (name.equals(t.getName())) {
        return new VirtualMachineSnapshot(vm.getServerConnection(), t.getSnapshot());
      }
      VirtualMachineSnapshot child = findInTree(vm, t.getChildSnapshotList(), name);
      if (child != null) {
        return child;
      }
    }
    return null;
  }
}
