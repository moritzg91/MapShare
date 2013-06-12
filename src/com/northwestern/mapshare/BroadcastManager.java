package com.northwestern.mapshare;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.northwestern.mapshare.BroadcastManager.Node;
import com.northwestern.mapshare.Scheduler.MapSegment;

public class BroadcastManager extends Thread{
	public List<Result> requestSegments(List<MapSegment> segments) {
		return null;
	}
	private static final String TAG = "BroadcastRequest";
	private static final String REMOTE_KEY = "MapShare";
	private static final int REQUEST_PORT = 2562;
	private static final int TIMEOUT_MS = 500;
	private boolean keep_running = true;
	
	private static final String mChallenge = "something";
	private WifiManager mWifi;
	final CountDownLatch latch = new CountDownLatch(1);
	public List<Phone_Result> Result_List = new ArrayList<Phone_Result>();
	
	interface MapShareReceiver {
		void addAnnouncedServers(InetAddress[] host, int port[]);
	}
	
	public BroadcastManager(WifiManager wifi) {
		mWifi = wifi;
	}
	public void waiting() throws InterruptedException{
		latch.await();
	}
	//Set up socket and 
	public void run() {
		try{
			DatagramSocket socket = new DatagramSocket(REQUEST_PORT);
			socket.setBroadcast(true);
			socket.setSoTimeout(TIMEOUT_MS);
			
			sendMapShareRequest(socket);
			listenForResponses(socket);
		} catch (IOException e) {
			Log.e(TAG, "Could not send mapshare request", e);
		}
		for (int i = 0; i <Result_List.size();i++) {
			Log.d("RESULTS", "Result IP: " + Result_List.get(i));
		}
		latch.countDown();
	}
	
	//Send a broadcast UDP packet with a request for mapshare services to
	//announce themselves
	private void sendMapShareRequest(DatagramSocket socket) throws IOException {
		String data = String.format(
				"<bdp1 cmd=\"mapshare\" application=\"map_share\" challenge=\"%s\" signature=\"%s\"/>",
				mChallenge, getSignature(mChallenge));
		Log.d(TAG, "Sending data " + data);
		
		DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
				getBroadcastAddress(), REQUEST_PORT);
		socket.send(packet);
	}
	
	//Calculate broadcast IP to send packet along. Apparently mobile network
	// won't do broadcast
	private InetAddress getBroadcastAddress() throws IOException {
		DhcpInfo dhcp = mWifi.getDhcpInfo();
		if (dhcp == null) {
			Log.d(TAG, "Could not get dhcp info");
			return null;
		}
		
		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte)((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads);
	}
	
	//Listen on socket for responses until timeout (TIMEOUT_MS)
	//@param socket
	//   socket on which the announcement request was sent
	//@throws IOException
	private void listenForResponses(DatagramSocket socket) throws IOException {
		byte[] buf = new byte[1024];
		try {
			while (true) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				String s = new String(packet.getData(), 0, packet.getLength());
				Log.d(TAG, "Received response " + s);
				Log.d(TAG, "IP is " + packet.getAddress().toString());
				Phone_Result result = new Phone_Result();
				result.ip_addr = packet.getAddress();
				Result_List.add(result);
				Log.d("RESULT LIST", "ADDED TO RESULT LIST");
			}
		} catch (SocketTimeoutException e) {
			Log.d(TAG, "Receive timed out");
		}
	}
	
	//Calculate signature we need to send with request. it's a string
	// containing hex md5sum of the challenge and REMOTE_KEY
	// Not sure if we really need it, but it can't hurt
	private String getSignature(String challenge) {
		MessageDigest digest;
		byte[] md5sum = null;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(challenge.getBytes());
			digest.update(REMOTE_KEY.getBytes());
			md5sum = digest.digest();
		} catch (NoSuchAlgorithmException e){
			e.printStackTrace();
		}
		
		StringBuffer hexString = new StringBuffer();
		for (int k = 0; k < md5sum.length; ++k) {
			String s = Integer.toHexString((int) md5sum[k] & 0xFF);
			if (s.length() == 1)
				hexString.append('0');
			hexString.append(s);
		}
		return hexString.toString();
	}
	/*
	public static void main(String[] args) {
		new BroadcastManager(null).start();
		while (true){
		}
	}*/
	
	public class Result {
		public LatLng topLeft;
		public double width;
		public double height;
		public Bitmap map_image;
		
		public Result(LatLng latlng, double w, double h, Bitmap bmp) {
			width = w;
			height = h;
			topLeft = latlng;
			map_image = bmp;
		}
	}
	public class Request {
		
	}
	
	public class Phone_Result {
		public InetAddress ip_addr;
		//int gsm_signal_strength;
		//int gsm_bit_error_rate;
	}
	
	public class Node {

		public void requestSegment(MapSegment mapSegment) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
