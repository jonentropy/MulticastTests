package com.example.multicastpagetest;


import android.content.Context;
import android.net.wifi.WifiManager;

public final class Tools {
	private static WifiManager.WifiLock mWifiHighPerfLock;
	private static WifiManager.MulticastLock mMulticastLock;

	private Tools(){}

	public synchronized  static void acquireWifiHighPerfLock(Context context) {
		if (mWifiHighPerfLock == null) {
			mWifiHighPerfLock = ((WifiManager)context.getSystemService(Context.WIFI_SERVICE))
					.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PAGER_LOCK");
			mWifiHighPerfLock.acquire();
		}
	}

	public synchronized static void releaseWifiHighPerfLock() {
		if (mWifiHighPerfLock != null) {
			mWifiHighPerfLock.release();
			mWifiHighPerfLock = null;
		}
	}

	public synchronized static void acquireMulticastLock(Context context){
		if(mMulticastLock == null){
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			mMulticastLock = wifi.createMulticastLock("multicastLock");
			mMulticastLock.setReferenceCounted(false);
			mMulticastLock.acquire();
		}
	}
	
	public synchronized static void releaseMulticastLock(){
		if(mMulticastLock != null){
			mMulticastLock.release();
			mMulticastLock = null;
		}
	}
}
