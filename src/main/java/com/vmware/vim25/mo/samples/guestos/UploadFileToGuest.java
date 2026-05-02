package com.vmware.vim25.mo.samples.guestos;

import com.vmware.vim25.GuestFileAttributes;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Uploads a local file into a powered-on VM's guest OS using the
 * GuestFileManager.initiateFileTransferToGuest API.
 *
 * <p>Pattern (mirrors what the doublecloud vmguest "putFile" helper did, GitHub issue #49):
 * <ol>
 *   <li>Authenticate to vCenter and locate the target VM.</li>
 *   <li>Authenticate to the guest OS via VMware Tools using NamePasswordAuthentication.</li>
 *   <li>Call initiateFileTransferToGuest to get a single-use upload URL with a "*" placeholder
 *       for the host that runs the VM.</li>
 *   <li>Substitute the actual ESXi host IP/hostname into the URL.</li>
 *   <li>HTTP PUT the file bytes to that URL.</li>
 * </ol>
 *
 * <p>Requires VMware Tools running in the guest. The guest user must have permission to
 * write to the destination path.
 *
 * <p>Usage:
 * <pre>
 * java UploadFileToGuest &lt;url&gt; &lt;vc-user&gt; &lt;vc-pass&gt; &lt;vm-name&gt;
 *     &lt;guest-user&gt; &lt;guest-pass&gt; &lt;local-file&gt; &lt;guest-path&gt;
 *
 * java UploadFileToGuest https://vc01/sdk administrator@vsphere.local vmware123
 *     test-vm root rootpw /tmp/hello.txt /tmp/hello.txt
 * </pre>
 */
public class UploadFileToGuest {

    private static final int BUFFER_SIZE = 64 * 1024;

    public static void main(String[] args) throws Exception {
        if (args.length != 8) {
            System.out.println("Usage: java UploadFileToGuest <url> <vc-user> <vc-pass> <vm-name>"
                + " <guest-user> <guest-pass> <local-file> <guest-path>");
            System.exit(0);
        }

        String vcUrl     = args[0];
        String vcUser    = args[1];
        String vcPass    = args[2];
        String vmName    = args[3];
        String guestUser = args[4];
        String guestPass = args[5];
        String localFile = args[6];
        String guestPath = args[7];

        File local = new File(localFile);
        if (!local.exists()) {
            throw new IllegalArgumentException("Local file not found: " + localFile);
        }

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

            // Empty GuestFileAttributes lets the guest pick reasonable defaults
            // (mode/owner on Linux; nothing on Windows). Populate explicitly if
            // you need specific permissions.
            GuestFileAttributes attrs = new GuestFileAttributes();

            // initiateFileTransferToGuest returns a URL with a "*" placeholder for the
            // host running the VM — vCenter expects the caller to substitute it.
            String uploadUrl = fileManager.initiateFileTransferToGuest(
                vm, creds, guestPath, attrs, local.length(), /*overwrite=*/true);

            String hostUrl = substituteHost(uploadUrl, vm);
            System.out.println("Upload URL: " + hostUrl);

            HttpsConnectionUtil.trustAllHosts();
            URL url = new URL(hostUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setHostnameVerifier(HttpsConnectionUtil.DO_NOT_VERIFY);
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setChunkedStreamingMode(BUFFER_SIZE);

            try (InputStream in = new BufferedInputStream(new FileInputStream(local));
                 OutputStream out = conn.getOutputStream()) {
                byte[] buf = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.flush();
            }

            int rc = conn.getResponseCode();
            System.out.println("HTTP " + rc + " " + conn.getResponseMessage());
            if (rc < 200 || rc >= 300) {
                throw new RuntimeException("Upload failed with HTTP " + rc);
            }
            System.out.println("Uploaded " + local.length() + " bytes to " + guestPath);
            conn.disconnect();
        } finally {
            si.getServerConnection().logout();
        }
    }

    /**
     * The transfer URL comes back with a "*" placeholder for the ESXi host running the VM.
     * Replace it with the host's name (vCenter clients can usually reach the host by its
     * managed name; for environments where they can't, swap in the management IP via
     * host.getSummary().getConfig() or HostSystem properties).
     */
    static String substituteHost(String urlWithStar, VirtualMachine vm) throws Exception {
        HostSystem host = new HostSystem(
            vm.getServerConnection(), vm.getRuntime().getHost());
        String hostName = host.getName();
        return urlWithStar.replace("*", hostName);
    }
}
