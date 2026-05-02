package com.vmware.vim25.mo.samples.guestos;

import com.vmware.vim25.FileTransferInformation;
import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.mo.GuestFileManager;
import com.vmware.vim25.mo.GuestOperationsManager;
import com.vmware.vim25.mo.GuestProcessManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.HttpsConnectionUtil;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * Runs a shell command in a powered-on VM's guest OS and prints its captured stdout and
 * stderr. Demonstrates the workaround for the vSphere Guest Operations API not directly
 * exposing process output: redirect into temp files inside the guest, wait for the
 * process to exit, then download the temp files back via initiateFileTransferFromGuest.
 *
 * <p>Pattern (mirrors what the doublecloud vmguest "runScript" / "runCommand" helpers did,
 * GitHub issue #49):
 * <ol>
 *   <li>Pick a shell appropriate for the guest family (cmd.exe vs /bin/sh).</li>
 *   <li>Compose the command so its stdout/stderr are redirected to two temp files in the guest.</li>
 *   <li>Start the process via GuestProcessManager.startProgramInGuest.</li>
 *   <li>Poll listProcessesInGuest until the process exits, capturing the exit code.</li>
 *   <li>Download the two temp files via GuestFileManager.initiateFileTransferFromGuest.</li>
 *   <li>Delete the temp files via deleteFileInGuest.</li>
 * </ol>
 *
 * <p>Requires VMware Tools running in the guest with a guest user that can run /bin/sh
 * or cmd.exe and write to /tmp (Linux) or %TEMP% (Windows).
 *
 * <p>Usage:
 * <pre>
 * java RunCommandInGuestAndCaptureOutput &lt;url&gt; &lt;vc-user&gt; &lt;vc-pass&gt; &lt;vm-name&gt;
 *     &lt;guest-user&gt; &lt;guest-pass&gt; &lt;command&gt;
 *
 * java RunCommandInGuestAndCaptureOutput https://vc01/sdk administrator@vsphere.local vmware123
 *     test-vm root rootpw "ls -la /var/log"
 * </pre>
 */
public class RunCommandInGuestAndCaptureOutput {

    private static final long POLL_INTERVAL_MS = 1000;
    private static final long MAX_WAIT_MS = 5 * 60 * 1000; // 5 minutes

    public static void main(String[] args) throws Exception {
        if (args.length != 7) {
            System.out.println("Usage: java RunCommandInGuestAndCaptureOutput <url> <vc-user> <vc-pass>"
                + " <vm-name> <guest-user> <guest-pass> <command>");
            System.exit(0);
        }

        String vcUrl     = args[0];
        String vcUser    = args[1];
        String vcPass    = args[2];
        String vmName    = args[3];
        String guestUser = args[4];
        String guestPass = args[5];
        String command   = args[6];

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

            String osFamily = vm.getGuest().getGuestFamily();
            boolean isWindows = osFamily != null && osFamily.contains("windowsGuest");
            System.out.println("Guest family: " + osFamily);

            NamePasswordAuthentication creds = new NamePasswordAuthentication();
            creds.username = guestUser;
            creds.password = guestPass;

            GuestOperationsManager gom = si.getGuestOperationsManager();
            GuestProcessManager pm = gom.getProcessManager(vm);
            GuestFileManager fm = gom.getFileManager(vm);

            // Pick guest-appropriate temp file paths and shell wrapping.
            String runId = "yavijava-" + System.currentTimeMillis();
            String stdoutPath;
            String stderrPath;
            GuestProgramSpec spec = new GuestProgramSpec();
            if (isWindows) {
                stdoutPath = "C:\\Windows\\Temp\\" + runId + ".out";
                stderrPath = "C:\\Windows\\Temp\\" + runId + ".err";
                spec.programPath = "C:\\Windows\\System32\\cmd.exe";
                spec.arguments = "/C " + command + " > \"" + stdoutPath + "\" 2> \"" + stderrPath + "\"";
            } else {
                stdoutPath = "/tmp/" + runId + ".out";
                stderrPath = "/tmp/" + runId + ".err";
                spec.programPath = "/bin/sh";
                spec.arguments = "-c \"" + command.replace("\"", "\\\"")
                    + " > " + stdoutPath + " 2> " + stderrPath + "\"";
            }

            System.out.println("Starting: " + spec.programPath + " " + spec.arguments);
            long pid = pm.startProgramInGuest(creds, spec);
            System.out.println("pid: " + pid);

            int exitCode = waitForExit(pm, creds, pid);
            System.out.println("exit code: " + exitCode);

            String stdout = downloadCapturedFile(fm, vm, creds, stdoutPath);
            String stderr = downloadCapturedFile(fm, vm, creds, stderrPath);

            System.out.println("---- STDOUT ----");
            System.out.print(stdout);
            System.out.println("---- STDERR ----");
            System.out.print(stderr);

            // Best-effort cleanup; ignore failures (file may already be gone).
            try { fm.deleteFileInGuest(vm, creds, stdoutPath); } catch (Exception ignored) {}
            try { fm.deleteFileInGuest(vm, creds, stderrPath); } catch (Exception ignored) {}

        } finally {
            si.getServerConnection().logout();
        }
    }

    private static int waitForExit(GuestProcessManager pm, NamePasswordAuthentication creds, long pid)
        throws Exception {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            GuestProcessInfo[] infos = pm.listProcessesInGuest(creds, new long[]{pid});
            if (infos != null && infos.length > 0 && infos[0].getExitCode() != null) {
                return infos[0].getExitCode();
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new RuntimeException("Timed out waiting for guest pid " + pid + " to exit");
    }

    private static String downloadCapturedFile(GuestFileManager fm, VirtualMachine vm,
                                               NamePasswordAuthentication creds, String guestPath)
        throws Exception {
        FileTransferInformation info = fm.initiateFileTransferFromGuest(vm, creds, guestPath);
        String hostUrl = UploadFileToGuest.substituteHost(info.getUrl(), vm);

        HttpsConnectionUtil.trustAllHosts();
        HttpsURLConnection conn = (HttpsURLConnection) new URL(hostUrl).openConnection();
        conn.setHostnameVerifier(HttpsConnectionUtil.DO_NOT_VERIFY);
        conn.setRequestMethod("GET");

        int rc = conn.getResponseCode();
        if (rc < 200 || rc >= 300) {
            throw new RuntimeException("Download of " + guestPath + " failed: HTTP " + rc);
        }

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            byte[] chunk = new byte[8 * 1024];
            int len;
            while ((len = in.read(chunk)) != -1) {
                buf.write(chunk, 0, len);
            }
        }
        conn.disconnect();
        return buf.toString("UTF-8");
    }
}
