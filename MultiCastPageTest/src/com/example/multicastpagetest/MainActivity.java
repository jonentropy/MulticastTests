package com.example.multicastpagetest;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	private AudioStream stream;
	private AudioGroup audioGroup;
	private AudioManager audioManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void init(View v){
		Tools.acquireMulticastLock(this);
		Tools.acquireWifiHighPerfLock(this);
		
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 
		audioGroup = new AudioGroup();
		audioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);   
		
		try {
			stream = new AudioStream(InetAddress.getByName("239.255.10.10"));
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		stream.setCodec(AudioCodec.PCMU);
		
		((TextView) findViewById(R.id.textView1)).setText(Integer.toString(stream.getLocalPort()));
		
	}
	
	public void receive(View v){

		stream.setMode(RtpStream.MODE_RECEIVE_ONLY);

		try {
			stream.associate(InetAddress.getByName("239.255.10.10"), Integer.parseInt(((EditText) findViewById(R.id.editText1)).getText().toString()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stream.join(audioGroup);
		
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		audioManager.setSpeakerphoneOn(true);
	
	}
	
	public void page(View v){
		
		stream.setMode(RtpStream.MODE_SEND_ONLY);
		
		try {
			stream.associate(InetAddress.getByName("239.255.10.10"), Integer.parseInt(((EditText) findViewById(R.id.editText1)).getText().toString()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stream.join(audioGroup);
		
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
	}
	
	public void stop(View v){
		System.exit(0);
	}

}
