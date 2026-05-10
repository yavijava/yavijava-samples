package com.vmware.vim25.mo.samples;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

import com.vmware.vim25.mo.ServiceInstance;

public class SampleUtil {

	public static ServiceInstance createServiceInstance() throws RemoteException, MalformedURLException {
	   return createServiceInstance(
	         requireSetting("sample.url", "YAVIJAVA_URL"),
	         requireSetting("sample.user", "YAVIJAVA_USER"),
	         requireSetting("sample.password", "YAVIJAVA_PASSWORD"));
	}

	public static ServiceInstance createServiceInstance(String url, String user, String password) throws RemoteException, MalformedURLException {
	   ServiceInstance si = new ServiceInstance(new URL(url), user, password, true);
	   String locale = localeSetting(System.getenv());
	   if (locale != null) {
	      si.getSessionManager().setLocale(locale);
	   }
	   return si;
	}

	static String requireSetting(String propertyName, String envName) {
	   return requireSetting(propertyName, envName, System.getenv());
	}

	static String requireSetting(String propertyName, String envName, Map<String, String> env) {
	   String value = setting(propertyName, envName, null, env);
	   if (value == null) {
	      throw new IllegalStateException("Set " + propertyName + " or " + envName + " before running this sample.");
	   }
	   return value;
	}

	static String localeSetting(Map<String, String> env) {
	   return setting("sample.locale", "YAVIJAVA_LOCALE", null, env);
	}

	static String setting(String propertyName, String envName, String defaultValue, Map<String, String> env) {
	   String value = System.getProperty(propertyName);
	   if (value == null) {
	      value = env.get(envName);
	   }
	   if (value == null) {
	      return defaultValue;
	   }
	   value = value.trim();
	   if (value.isEmpty() || value.equals("changeme") || value.contains("example.com")) {
	      throw new IllegalStateException("Set a real value for " + propertyName + " or " + envName + ".");
	   }
	   return value;
	}

}
