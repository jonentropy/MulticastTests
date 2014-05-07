package com.example.multicasttest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	EditText tv;
	private MulticastLock multicastLock;
	private Thread runit = new Thread(){
		@Override
		public void run(){
			MulticastSocket socket = null;
			try {
				socket = new MulticastSocket(1235);
				socket.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getByName(MainActivity.this.getIP())));
				socket.joinGroup(InetAddress.getByName("239.255.10.10"));
				socket.setSoTimeout(100);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if(socket == null) return;
			
			final byte[] buf = new byte[1024];
			final DatagramPacket reply = new DatagramPacket(buf, buf.length);
			
			while(!Thread.currentThread().isInterrupted()) {
				
				try {
					reply.setData(buf); //needed on some Android OSs to avoid message being cut off at previous packet's size
					socket.receive(reply);

					MainActivity.this.runOnUiThread(new Runnable(){
						@Override
						public void run() {
							tv.append(" " + new String(reply.getData()).trim());
						}
					});

				} catch (IOException e) {
					MainActivity.this.runOnUiThread(new Runnable(){
						@Override
						public void run() {
							//tv.append(".");
						}
					});					
				}
			}
		}
	};
	private boolean started;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (EditText) findViewById(R.id.editText1);
		TextView v = (TextView) (findViewById(R.id.textView1));
		v.setText(getIP());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void getIt(View v){
		if(!started){
			v.setEnabled(false);
			WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			multicastLock = wifi.createMulticastLock("multicastLock");
			multicastLock.setReferenceCounted(false);
			multicastLock.acquire();

			tv.setText("");

			runit.start();
			started = true;
		}
	}

	public void stopIt(View v){
		if(multicastLock != null) multicastLock.release();
		
		if(runit != null && runit.isAlive()) runit.interrupt();
		finish();
	}

	public String getIP(){
		WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		return Formatter.formatIpAddress(ip);
	}

}
