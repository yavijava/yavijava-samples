package com.vmware.vim25.mo.samples.guestos;

import com.vmware.vim25.FileTransferInformation;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.mo.GuestFileManager;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.HttpsConnectionUtil;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Downloads a file from a powered-on VM's guest OS to local disk using the
 * GuestFileManager.initiateFileTransferFromGuest API.
 *
 * <p>Pattern (mirrors what the doublecloud vmguest "getFile" helper did, GitHub issue #49):
 * <ol>
 *   <li>Authenticate to vCenter and locate the target VM.</li>
 *   <li>Authenticate to the guest OS via VMware Tools using NamePasswordAuthentication.</li>
 *   <li>Call initiateFileTransferFromGuest to get a {@link FileTransferInformation} with a
 *       single-use download URL (with "*" placeholder for the VM's host) and the size in bytes.</li>
 *   <li>Substitute the actual ESXi host IP/hostname into the URL.</li>
 *   <li>HTTP GET the bytes and stream them to a local file.</li>
 * </ol>
 *
 * <p>Requires VMware Tools running in the guest. The guest user must have permission to
 * read the source path.
 *
 * <p>Usage:
 * <pre>
 * java DownloadFileFromGuest &lt;url&gt; &lt;vc-user&gt; &lt;vc-pass&gt; &lt;vm-name&gt;
 *     &lt;guest-user&gt; &lt;guest-pass&gt; &lt;guest-path&gt; &lt;local-file&gt;
 *
 * java DownloadFileFromGuest https://vc01/sdk administrator@vsphere.local vmware123
 *     test-vm root rootpw /var/log/messages /tmp/messages
 * </pre>
 */
public class DownloadFileFromGuest {

    private static final int BUFFER_SIZE = 64 * 1024;

    public static void main(String[] args) throws Exception {
        if (args.length != 8) {
            System.out.println("Usage: java DownloadFileFromGuest <url> <vc-user> <vc-pass> <vm-name>"
                + " <guest-user> <guest-pass> <guest-path> <local-file>");
            System.exit(0);
        }

        String vcUrl     = args[0];
        String vcUser    = args[1];
        String vcPass    = args[2];
        String vmName    = args[3];
        String guestUser = args[4];
        String guestPass = args[5];
        String guestPath = args[6];
        String localFile = args[7];

        ServiceInstance si = new ServiceInstance(new URL(vcUrl), vcUser, vcPass, true);
        try {
            VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder())
                .searchManagedEntity("VirtualMachine", vmName);
            if (vm == null) {
                System.out.println("VM not found: " + vmName);
                return;
            }
            if (!"guestToolsRunning".equals(vm.getGuest().toolsRunningStatus)) {
                System.out.println("VMware Tools not running in guest; cannot proceed.");
                return;
            }

            NamePasswordAuthentication creds = new NamePasswordAuthentication();
            creds.username = guestUser;
            creds.password = guestPass;

            GuestOperationsManager gom = si.getGuestOperationsManager();
            GuestFileManager fileManager = gom.getFileManager(vm);

            FileTransferInformation info = fileManager.initiateFileTransferFromGuest(
                vm, creds, guestPath);

            String hostUrl = UploadFileToGuest.substituteHost(info.getUrl(), vm);
            System.out.println("Download URL: " + hostUrl);
            System.out.println("Reported size: " + info.getSize() + " bytes");

            HttpsConnectionUtil.trustAllHosts();
            URL url = new URL(hostUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setHostnameVerifier(HttpsConnectionUtil.DO_NOT_VERIFY);
            conn.setRequestMethod("GET");

            int rc = conn.getResponseCode();
            if (rc < 200 || rc >= 300) {
                throw new RuntimeException("Download failed with HTTP " + rc
                    + " " + conn.getResponseMessage());
            }

            long total = 0;
            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(localFile))) {
                byte[] buf = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    total += len;
                }
                out.flush();
            }
            System.out.println("Downloaded " + total + " bytes to " + localFile);
            conn.disconnect();
        } finally {
            si.getServerConnection().logout();
        }
    }
}
