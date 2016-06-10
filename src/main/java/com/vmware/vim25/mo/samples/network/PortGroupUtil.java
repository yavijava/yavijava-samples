package com.vmware.vim25.mo.samples.network;

import java.io.IOException;
import java.rmi.RemoteException;

import com.vmware.vim25.HostNetworkPolicy;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.HostNetworkSystem;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.Network;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.samples.SampleUtil;

public class PortGroupUtil {

    public static void main(String[] args) throws Exception {
	ServiceInstance si = SampleUtil.createServiceInstance();

	String clusterName = "cluster1";
	String hostName = "hostname1.domain.com";
	PortGroupUtil portGroup = new PortGroupUtil();
	portGroup.listPortGroup(si);
	portGroup.listPortGroupForHost(si, hostName);
	portGroup.listPortGroupForCluster(si, clusterName);

	si.getServerConnection().logout();
    }

    public void addPortGroup(ServiceInstance si, String hostName, String switchName, String portGroupName) throws IOException {
        HostSystem host = (HostSystem) (new InventoryNavigator(si.getRootFolder())).searchManagedEntity("HostSystem", hostName);
	if (host == null) {
	    System.out.println("Not found host:" + hostName);
	    return;
	}
        HostNetworkSystem network = host.getHostNetworkSystem();

        HostPortGroupSpec portGroupSpec = new HostPortGroupSpec();
        portGroupSpec.setName(portGroupName);
        portGroupSpec.setVlanId(0);
        portGroupSpec.setVswitchName(switchName);
        portGroupSpec.setPolicy(new HostNetworkPolicy());

        network.addPortGroup(portGroupSpec);
        System.out.println("Create port group:" + portGroupName + " on switch:" + switchName + " successfully!");
    }

    public void removePortGroup(ServiceInstance si, String hostName, String portGroupName) throws IOException {
        HostSystem host = (HostSystem) (new InventoryNavigator(si.getRootFolder())).searchManagedEntity("HostSystem", hostName);
	if (host == null) {
	    System.out.println("Not found host:" + hostName);
	    return;
	}
        HostNetworkSystem network = host.getHostNetworkSystem(); 
        network.removePortGroup(portGroupName);
        System.out.println("Remove port group:" + portGroupName + " successfully!");
    }

    public void listPortGroup(ServiceInstance si) throws InvalidProperty, RuntimeFault, RemoteException {
	ManagedEntity[] networks = (new InventoryNavigator(si.getRootFolder())).searchManagedEntities("Network");
	if (networks != null) {
	    System.out.println("List all port groups:");
	    for (int i = 0; i < networks.length; i++) {
		Network network = (Network) networks[i];
		System.out.println(i + ": " + network.getName());
	    }
	}
    }

    public void listPortGroupForHost(ServiceInstance si, String hostName) throws InvalidProperty, RuntimeFault, RemoteException {
        HostSystem host = (HostSystem) (new InventoryNavigator(si.getRootFolder())).searchManagedEntity("HostSystem", hostName);
	if (host == null) {
	    System.out.println("Not found host:" + hostName);
	    return;
	}
	System.out.println("List port groups for host:" + hostName);
	Network[] networks = host.getNetworks();
	for (int i = 0; i < networks.length; i++) {
	    System.out.println(i + ": " + networks[i].getName());
	}
    }

    public void listPortGroupForCluster(ServiceInstance si, String clusterName) throws InvalidProperty, RuntimeFault, RemoteException {
	ClusterComputeResource cluster = (ClusterComputeResource) (new InventoryNavigator(si.getRootFolder())).searchManagedEntity("ClusterComputeResource", clusterName);
	if (cluster == null) {
	    System.out.println("Not found cluster:" + cluster);
	    return;
	}
	System.out.println("List port groups for cluster:" + clusterName);
	Network[] networks = cluster.getNetworks();
	for (int i = 0; i < networks.length; i++) {
	    System.out.println(i + ": " + networks[i].getName());
	}
    }

}
