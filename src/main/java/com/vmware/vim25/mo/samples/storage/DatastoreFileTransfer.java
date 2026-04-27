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

import com.vmware.vim25.DatastoreHostMount;
import com.vmware.vim25.SessionManagerGenericServiceTicket;
import com.vmware.vim25.SessionManagerHttpServiceRequestSpec;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedObject;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.samples.HttpsConnectionUtil;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Demonstrates uploading a local file to a vSphere datastore and downloading
 * a file from a datastore using the vSphere HTTP datastore access API with a
 * generic service ticket for authentication (no SOAP session cookie required).
 *
 * Usage:
 *   java DatastoreFileTransfer &lt;url&gt; &lt;username&gt; &lt;password&gt;
 *       &lt;datacenter-name&gt; &lt;datastore-name&gt; &lt;local-file&gt;
 *       &lt;remote-path&gt; &lt;upload|download&gt;
 *
 * Example (upload):
 *   java DatastoreFileTransfer https://vc01/sdk administrator@vsphere.local vmware123
 *       DC0 datastore1 /tmp/hello.txt hello/hello.txt upload
 *
 * Example (download):
 *   java DatastoreFileTransfer https://vc01/sdk administrator@vsphere.local vmware123
 *       DC0 datastore1 /tmp/hello.txt hello/hello.txt download
 *
 * The remote-path is relative to the datastore root, e.g. "folder/file.txt".
 *
 * Background: GitHub issue #239 — community-contributed pattern using
 * SessionManager.acquireGenericServiceTicket() with a vmware_client_session_id
 * cookie header instead of the SOAP session cookie, enabling stateless HTTP
 * access to the datastore browser endpoint.
 *
 * http://vijava.sf.net
 * @author yavijava contributors
 */
public class DatastoreFileTransfer {

    private static final int BUFFER_SIZE = 64 * 1024; // 64 KB

    public static void main(String[] args) throws Exception {
        if (args.length != 8) {
            System.out.println("Usage: java DatastoreFileTransfer <url> <username> <password>"
                    + " <datacenter-name> <datastore-name> <local-file> <remote-path>"
                    + " <upload|download>");
            System.out.println();
            System.out.println("  url            vCenter SDK endpoint, e.g. https://vc01/sdk");
            System.out.println("  datacenter-name  name of the datacenter that owns the datastore");
            System.out.println("  datastore-name   name of the target datastore");
            System.out.println("  local-file       absolute path to the local file");
            System.out.println("  remote-path      path relative to datastore root, e.g. folder/file.txt");
            System.out.println("  upload|download  direction of transfer");
            System.exit(0);
        }

        String vcUrl       = args[0];
        String username    = args[1];
        String password    = args[2];
        String dcName      = args[3];
        String dsName      = args[4];
        String localFile   = args[5];
        String remotePath  = args[6];
        String direction   = args[7].toLowerCase();

        if (!direction.equals("upload") && !direction.equals("download")) {
            System.out.println("Error: last argument must be 'upload' or 'download'");
            System.exit(1);
        }

        // 1. Connect to vCenter.
        ServiceInstance si = new ServiceInstance(new URL(vcUrl), username, password, true);
        System.out.println("Connected to vCenter: " + vcUrl);

        try {
            Folder rootFolder = si.getRootFolder();

            // 2. Find the target Datastore by name.
            Datastore datastore = (Datastore) new InventoryNavigator(rootFolder)
                    .searchManagedEntity("Datastore", dsName);
            if (datastore == null) {
                System.out.println("Error: datastore '" + dsName + "' not found.");
                return;
            }
            System.out.println("Found datastore: " + dsName);

            // 3. Get the IP of a host that mounts this datastore.
            DatastoreHostMount[] mounts = datastore.getHost();
            if (mounts == null || mounts.length == 0) {
                System.out.println("Error: no hosts found for datastore '" + dsName + "'.");
                return;
            }

            // Resolve the host MOR to a HostSystem and retrieve its management IP.
            HostSystem host = new HostSystem(
                    si.getServerConnection(), mounts[0].getKey());
            String hostIp = host.getSummary().managementServerIp;
            if (hostIp == null || hostIp.isEmpty()) {
                // Fall back to the host's display name if no management IP is set.
                hostIp = host.getName();
            }
            System.out.println("Using host: " + hostIp);

            // 4. Build the datastore browser URL.
            //    Pattern: https://<host-ip>/folder/<remote-path>?dcPath=<dc-name>&dsName=<ds-name>
            String datastoreUrl = "https://" + hostIp + "/folder/"
                    + remotePath.replaceAll(" ", "%20")
                    + "?dcPath=" + dcName.replaceAll(" ", "%20")
                    + "&dsName=" + dsName.replaceAll(" ", "%20");
            System.out.println("Datastore URL: " + datastoreUrl);

            // 5. Acquire a generic service ticket for this URL.
            //    The ticket is single-use and grants access without a SOAP session cookie.
            SessionManagerHttpServiceRequestSpec ticketSpec =
                    new SessionManagerHttpServiceRequestSpec();
            ticketSpec.setUrl(datastoreUrl);
            ticketSpec.setMethod(direction.equals("upload") ? "httpPut" : "httpGet");

            SessionManagerGenericServiceTicket ticket =
                    si.getSessionManager().acquireGenericServiceTicket(ticketSpec);
            System.out.println("Acquired service ticket: " + ticket.getId());

            // 6. Open an HttpsURLConnection, trusting all certificates (development convenience).
            //    In production, configure a proper TrustManager with the host's certificate.
            HttpsConnectionUtil.trustAllHosts();
            URL url = new URL(datastoreUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setHostnameVerifier(HttpsConnectionUtil.DO_NOT_VERIFY);

            // Attach the service ticket as a cookie header.
            conn.setRequestProperty("Cookie", "vmware_client_session_id=" + ticket.getId());

            if (direction.equals("upload")) {
                uploadFile(conn, localFile);
            } else {
                downloadFile(conn, localFile);
            }

            conn.disconnect();
            System.out.println("Transfer complete.");

        } finally {
            si.getServerConnection().logout();
        }
    }

    /**
     * Uploads localFilePath to the datastore via HTTP PUT.
     */
    private static void uploadFile(HttpsURLConnection conn, String localFilePath) throws Exception {
        File file = new File(localFilePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Local file not found: " + localFilePath);
        }

        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setChunkedStreamingMode(BUFFER_SIZE);

        System.out.println("Uploading '" + localFilePath + "' (" + file.length() + " bytes) ...");

        try (InputStream in = new BufferedInputStream(new FileInputStream(file));
             OutputStream out = conn.getOutputStream()) {
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        }

        int responseCode = conn.getResponseCode();
        System.out.println("Server response: " + responseCode + " " + conn.getResponseMessage());
        if (responseCode < 200 || responseCode >= 300) {
            throw new RuntimeException("Upload failed with HTTP " + responseCode);
        }
    }

    /**
     * Downloads a file from the datastore via HTTP GET and saves it to localFilePath.
     */
    private static void downloadFile(HttpsURLConnection conn, String localFilePath) throws Exception {
        conn.setDoInput(true);
        conn.setRequestMethod("GET");

        System.out.println("Downloading to '" + localFilePath + "' ...");

        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new RuntimeException("Download failed with HTTP " + responseCode
                    + " " + conn.getResponseMessage());
        }

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(localFilePath))) {
            byte[] buf = new byte[BUFFER_SIZE];
            long totalBytes = 0;
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                totalBytes += len;
            }
            out.flush();
            System.out.println("Downloaded " + totalBytes + " bytes.");
        }
    }
}
