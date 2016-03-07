package com.vmware.vim25.mo.samples;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpsConnectionUtil {

   public final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
         return true;
      }
   };

   public static void trustAllHosts() {
      TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
         public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
         }

         public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
               throws java.security.cert.CertificateException {
         }

         public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
               throws java.security.cert.CertificateException {
         }
      } };

      try {
         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(null, trustAllCerts, new java.security.SecureRandom());
         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
