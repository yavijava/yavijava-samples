package com.vmware.vim25.mo.samples.storage;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.mo.samples.HttpsConnectionUtil;
import com.vmware.vim25.mo.samples.SampleUtil;

import java.io.*;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;

public class DatastoreFileOps {

   private Datastore retrieveDatastore(InventoryNavigator in) throws RemoteException {
      return (Datastore) in.searchManagedEntities("Datastore")[0];
   }

   private Datacenter retrieveDatacenter(InventoryNavigator in) throws RemoteException {
      return (Datacenter) in.searchManagedEntities("Datacenter")[0];
   }

   private ResourcePool retrieveResourcePool(InventoryNavigator in) throws RemoteException {
      return (ResourcePool) in.searchManagedEntities("ResourcePool")[0];
   }

   private Network retrieveNetwork(InventoryNavigator in) throws RemoteException {
      return (Network) in.searchManagedEntities("Network")[0];
   }

   public void uploadFileToStore() throws Exception {

      ServiceInstance si = SampleUtil.createServiceInstance();
      Folder rootFolder = si.getRootFolder();

      InventoryNavigator inv = new InventoryNavigator(rootFolder);

      Datacenter dc = retrieveDatacenter(inv);
      String datacenter = dc.getName();

      String url = si.getServerConnection().getUrl().toString();
      String serviceUrl =  url.substring(0, url.lastIndexOf("sdk") - 1);
      String httpUrl = serviceUrl + "/folder/test.txt?dcPath=" + datacenter + "&dsName=datastore1";
      httpUrl = httpUrl.replaceAll("\\ ", "%20");
      System.out.println("Uploading local file to " + httpUrl);
      URL fileURL = new URL(httpUrl);
      HttpsConnectionUtil.trustAllHosts();
      HttpsURLConnection conn = (HttpsURLConnection) fileURL.openConnection();
      conn.setHostnameVerifier(HttpsConnectionUtil.DO_NOT_VERIFY);
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setAllowUserInteraction(true);

      // Extract cookie from ServiceInstance connection.
      VimPortType vimPort = si.getServerConnection().getVimService();
      String cookieValue = vimPort.getWsc().getCookie();
      StringTokenizer tokenizer = new StringTokenizer(cookieValue, ";");
      cookieValue = tokenizer.nextToken();
      String path = "$" + tokenizer.nextToken();
      String cookie = "$Version=\"1\"; " + cookieValue + "; " + path;

      String localFilePath = "/tmp/test.txt";

      conn.setRequestProperty("Cookie", cookie);
      conn.setRequestProperty("Content-Type", "application/octet-stream");
      conn.setRequestMethod("PUT");
      conn.setRequestProperty("Content-Length", "1024");
      long fileLen = new File(localFilePath).length();
      System.out.println("File size is: " + fileLen);
      conn.setChunkedStreamingMode((int) fileLen);
      OutputStream out = conn.getOutputStream();
      InputStream in = new BufferedInputStream(new FileInputStream(localFilePath));
      int bufLen = 9 * 1024;
      byte[] buf = new byte[bufLen];
      byte[] tmp = null;
      int len = 0;
      while ((len = in.read(buf, 0, bufLen)) != -1) {
         tmp = new byte[len];
         System.arraycopy(buf, 0, tmp, 0, len);
         out.write(tmp, 0, len);
      }
      in.close();
      out.close();
      System.out.println(conn.getResponseMessage());
      conn.disconnect();
   }

   public static void main(String[] args) throws Exception {
      DatastoreFileOps obj = new DatastoreFileOps();
      obj.uploadFileToStore();
   }

}
