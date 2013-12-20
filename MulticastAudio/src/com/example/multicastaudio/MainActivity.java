package com.example.multicastaudio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {
	private static final String MULTICAST_IP = "239.255.12.12";
	private static final int port = 42153;
	private static final int freq = 8000;
	private static int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private int bufferSize = 2048;

	private static final ExecutorService threadPool = Executors.newCachedThreadPool();
	MulticastSocket socket;
	private volatile boolean sending;
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;
		setContentView(R.layout.activity_main);
		startServer();
	}

	/**
	 * start listeneing for packets. if audio packets arrive, play them,
	 * if none appear, shut the audio down.
	 */
	private void startServer() {

		sending = false;
		Tools.acquireMulticastLock(this);

		try {
			socket = new MulticastSocket(port);
			socket.setReuseAddress(true);
			socket.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getByName(MainActivity.this.getIP())));
			socket.joinGroup(InetAddress.getByName(MULTICAST_IP));
			socket.setSoTimeout(200);

		} catch (IOException e) {
			e.printStackTrace();
		}

		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 
		audioManager.setMode(AudioManager.MODE_NORMAL);

		threadPool.execute(new Runnable(){
			@Override
			public void run() {
				AudioTrack audioTrack = null;

				final byte[] buf = new byte[bufferSize];

				while (!Thread.currentThread().isInterrupted()) {
					if(!sending){
						try {
							final DatagramPacket reply = new DatagramPacket(buf, buf.length);
							try {
								reply.setData(buf); //needed on some Android OSs to avoid message being cut off at previous packet's size
								socket.receive(reply);

								if(audioTrack == null){
									Tools.acquireWifiHighPerfLock(context);
									//set up new track to play back audio with 
									Log.i("PAGER", "Setting up AudioTrack...");
									audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, freq, AudioFormat.CHANNEL_OUT_MONO, ENCODING, bufferSize, AudioTrack.MODE_STREAM);

									if (audioTrack == null || audioTrack.getState() != AudioTrack.STATE_INITIALIZED) throw new IllegalStateException("Could not create AudioTrack for paging.");

									audioTrack.setPlaybackRate(freq);
									audioTrack.play();
								}

								audioTrack.write(buf, 0, reply.getLength());
							}
							catch (SocketTimeoutException e) {
								//Socket timed out, so close audio and release wifi high performance lock
								if(audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED){
									Log.i("PAGER", "Closing AudioTrack...");
									audioTrack.stop();
									audioTrack.release();
									audioTrack = null;
									Tools.releaseWifiHighPerfLock();
								}
							}
						} 
						catch (Throwable t) {
							Log.e("PAGER", "error capturing audio", t);
						}
					}
					else{
						if(audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED){
							Log.i("PAGER", "Closing AudioTrack...");
							audioTrack.stop();
							audioTrack.release();
							audioTrack = null;
							//do not release wifi high performance lock, as we are still sending
						}
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		});
	}

	/**
	 * page out
	 * @param v
	 */
	public void start(View v){
		
		final AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, freq,
				AudioFormat.CHANNEL_IN_MONO,
				ENCODING, bufferSize);

		if (audioRecorder == null || audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
			throw new IllegalStateException("Could not create AudioRecord for paging.");
		}
		//	audioManager.setSpeakerphoneOn(false);

		threadPool.execute(new Runnable(){
			@Override
			public void run() {
				sending = true;
				final byte[] buffer = new byte[bufferSize];

				audioRecorder.startRecording();

				Tools.acquireWifiHighPerfLock(context);
				
				while (!Thread.currentThread().isInterrupted()) {
					try {
						int bytesRead = audioRecorder.read(buffer, 0, bufferSize);  

						try {
							socket.send(new DatagramPacket(buffer, bytesRead, InetAddress.getByName(MULTICAST_IP), port));
						} catch (Exception e) {
							e.printStackTrace();
						}
					} 
					catch (Throwable t) {
						Log.e("PAGER", "error capturing audio", t);
					}
				}
				Tools.releaseWifiHighPerfLock();
			}
		});
	}

	/**
	 * stop paging out
	 * @param v
	 */
	public void stop(View v){
		threadPool.shutdownNow();
	}

	public String getIP(){
		WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		return Formatter.formatIpAddress(ip);
	}

}
