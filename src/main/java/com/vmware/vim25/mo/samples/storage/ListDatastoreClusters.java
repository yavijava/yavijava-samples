/*================================================================================
Copyright (c) 2008 VMware, Inc. All Rights Reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

 * Neither the name of VMware, Inc. nor the names of its contributors may be used
to endorse or promote products derived from this software without specific prior
written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL VMWARE, INC. OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
================================================================================*/

package com.vmware.vim25.mo.samples.storage;

import java.net.URL;

import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.StoragePod;

/**
 * Connects to a vCenter server and lists all Datastore Clusters (StoragePods)
 * along with the Datastores contained within each cluster.
 *
 * Usage: java ListDatastoreClusters &lt;url&gt; &lt;username&gt; &lt;password&gt;
 *
 * Based on community code from GitHub issue #246.
 *
 * http://vijava.sf.net
 * @author Steve Jin
 */

public class ListDatastoreClusters
{
  public static void main(String[] args) throws Exception
  {
    if (args.length != 3)
    {
      System.out.println("Usage: java ListDatastoreClusters <url> <username> <password>");
      System.exit(0);
    }

    ServiceInstance si = new ServiceInstance(
        new URL(args[0]), args[1], args[2], true);

    ManagedEntity[] storagePods = new InventoryNavigator(
        si.getRootFolder()).searchManagedEntities(
            new String[][] { {"StoragePod", "name"} }, true);

    if (storagePods == null || storagePods.length == 0)
    {
      System.out.println("No Datastore Clusters found.");
    }
    else
    {
      for (ManagedEntity entity : storagePods)
      {
        StoragePod storagePod = (StoragePod) entity;
        System.out.println("Datastore Cluster: " + storagePod.getName());

        ManagedEntity[] children = storagePod.getChildEntity();
        if (children != null)
        {
          for (ManagedEntity child : children)
          {
            if (child instanceof Datastore)
            {
              Datastore datastore = (Datastore) child;
              System.out.println("  --> Datastore: " + datastore.getName());
            }
          }
        }
      }
    }

    si.getServerConnection().logout();
  }
}
