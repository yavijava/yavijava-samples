package com.vmware.vim25.mo.samples;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import com.vmware.vim25.mo.ServiceInstance;

public class SampleUtil {

	public static ServiceInstance createServiceInstance() throws RemoteException, MalformedURLException {
	   ServiceInstance si = new ServiceInstance(new URL("https://10.141.72.223/sdk"), "root", "vmware", true);
	   si.getSessionManager().setLocale("zh-CN"); // set locale for the content of all API result.
	   return si;
	}

}
