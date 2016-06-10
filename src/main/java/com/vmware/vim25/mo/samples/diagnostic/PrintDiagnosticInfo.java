package com.vmware.vim25.mo.samples.diagnostic;

import com.vmware.vim25.ArrayOfDiagnosticManagerBundleInfo;
import com.vmware.vim25.DiagnosticManagerBundleInfo;
import com.vmware.vim25.DiagnosticManagerLogDescriptor;
import com.vmware.vim25.DiagnosticManagerLogHeader;
import com.vmware.vim25.mo.DiagnosticManager;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.samples.SampleUtil;

public class PrintDiagnosticInfo {

    public static void main(String[] args) throws Exception {
	ServiceInstance si = SampleUtil.createServiceInstance();

	PrintDiagnosticInfo diagInfo = new PrintDiagnosticInfo();
	//print log descriptors.
	diagInfo.printLogDescriptor(si, null);
	//print log content.
	diagInfo.printLog(si, null, "vpxd:vpxd_cfg.log");
	//generate log bundles and print the URL of the bundles.
	ManagedEntity[] entities = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("HostSystem");
	HostSystem[] hosts = null;
	if (entities != null) {
	    hosts = new HostSystem[entities.length];
	    for (int i = 0; i < entities.length; i++) {
		hosts[i] = (HostSystem) entities[i];
	    }
	}
	diagInfo.generateLogBundles(si, false, hosts);

	si.getServerConnection().logout();
    }

    /**
     * Print the information of diagnostic files for a given system.
     * @param si ServiceInstance object.
     * @param hostName Specifies the host. If not specified, then it defaults to the server itself, for example, the VirtualCenter.
     */
    public void printLogDescriptor(ServiceInstance si, String hostName) throws Exception {
	HostSystem host = null;
	if (hostName != null) {
	    host = (HostSystem) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("HostSystem", hostName);
	    if (host == null) {
		throw new Exception("Not found host:" + hostName);
	    }
	}
	DiagnosticManager diagMgr = si.getDiagnosticManager();
	DiagnosticManagerLogDescriptor[] logDeses = diagMgr.queryDescriptions(host);
	System.out.println("============ Log Descriptors ============");
	for (DiagnosticManagerLogDescriptor logDes : logDeses) {
	    System.out.println("creator=" + logDes.getCreator() + ", info=" + logDes.getInfo() + ", fileName=" + logDes.getFileName()
	    + ", format=" + logDes.getFormat() + ", key=" + logDes.getKey() + ", mimeType=" + logDes.getMimeType());
	}
    }

    /**
     * Print part of the log. Log entries are always returned chronologically, typically with the newest event last.
     * @param si ServiceInstance object.
     * @param hostName Specifies the host. If not specified, then it defaults to the server itself, for example, the VirtualCenter.
     * @param key The key of the log file. It can be obtained using the DiagnosticManager.queryDescriptions method.
     */
    public void printLog(ServiceInstance si, String hostName, String key) throws Exception {
	HostSystem host = null;
	if (hostName != null) {
	    host = (HostSystem) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("HostSystem", hostName);
	    if (host == null) {
		throw new Exception("Not found host:" + hostName);
	    }
	}
	DiagnosticManager diagMgr = si.getDiagnosticManager();
	System.out.println("============ Log Content ============");
	int lines = 500; //The number of lines to browse once.
	for (int i = 1; ; i += lines) {
	    DiagnosticManagerLogHeader header = diagMgr.browseDiagnosticLog(host, key, i, lines);
	    String[] lineTexts = header.getLineText();
	    if (lineTexts != null) {
		for (String line : lineTexts) {
		    System.out.println(line);
		}
	    } else {
		break;
	    }
	}
    }

    /**
     * Generate the log bundles for the specific hosts. After the task done, print all the URLs of the generated bundles.
     * @param si ServiceInstance object.
     * @param includeDefault Specifies if the bundle should include the default server.
     * @param hosts Lists hosts that are included.
     */
    public void generateLogBundles(ServiceInstance si, boolean includeDefault, HostSystem[] hosts) throws Exception {
	DiagnosticManager diagMgr = si.getDiagnosticManager();
	Task task = diagMgr.generateLogBundles_Task(includeDefault, hosts);
	String result = task.waitForTask();
	if (Task.SUCCESS.equals(result)) {
	    ArrayOfDiagnosticManagerBundleInfo bundleArray = (ArrayOfDiagnosticManagerBundleInfo) task.getTaskInfo().getResult();
	    DiagnosticManagerBundleInfo[] bundles = bundleArray.getDiagnosticManagerBundleInfo();
	    if (bundles != null) {
		System.out.println("============ Log URL ============");
		for (DiagnosticManagerBundleInfo bundle : bundles) {
		    System.out.println(bundle.getUrl());
		}
	    } else {
		System.out.println("No bundle generated.");
	    }
	} else {
	    throw new Exception("Generate log bundles failed: error: " + task.getTaskInfo().getError().getLocalizedMessage());
	}
    }

}
