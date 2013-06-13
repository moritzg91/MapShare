package com.northwestern.mapshare;

import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

import android.telephony.SignalStrength;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;

public class BroadcastManager extends Thread{
	
	public enum SerializableTypes {
		REQUEST_T, 
		TILE_REQUEST_T;
	}
	
	public List<Result> requestSegments(List<Request> requests) {
		return null;
	}
	private static final String TAG = "BroadcastRequest";
	private static final String REMOTE_KEY = "MapShare";
	private static final int REQUEST_PORT = 2562;
	private static final int TIMEOUT_MS = 500;
	private boolean keep_running = true;
	private Context context;
	
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
	
	public BroadcastManager(WifiManager wifi, Context c) {
		mWifi = wifi;
		context = c;
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
			
			sendMapShareRequest(socket,new Request("GetPeersRequest",SerializableTypes.REQUEST_T));
			listenForResponses(socket);
		} catch (IOException e) {
			Log.e(TAG, "Could not send mapshare request", e);
		}
		for (int i = 0; i <Result_List.size();i++) {
			Log.d("RESULTS", "Result IP: " + Result_List.get(i));
		}
		latch.countDown();
	}
	
	public void listen_for_broadcasts(){
		try{
			DatagramSocket mSocket = new DatagramSocket(null);
			mSocket.setReuseAddress(true);
			mSocket.setBroadcast(true);
			mSocket.bind(new InetSocketAddress(REQUEST_PORT));
				//socket.setSoTimeout(TIMEOUT_MS);
				
			//sendMapShareRequest(socket);
			listenForBroadcasts(mSocket);
		} catch (IOException e) {
			Log.e(TAG, "listening for broadcasts", e);
		}
		
	}
	
	//Send a broadcast UDP packet with a request for mapshare services to
	//announce themselves
	private void sendMapShareRequest(DatagramSocket socket, Request req) throws IOException {

		byte[] reqBytes = req.toByteArray();
		Log.d(TAG, "Sending data " + req.toString() + "\0");
		
		DatagramPacket packet = new DatagramPacket(reqBytes, reqBytes.length,
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
				
				Request o = requestFromByteArray(packet.getData());
				
				Log.d(TAG, "Received response " + o.toString());
				Log.d(TAG, "IP is " + packet.getAddress().toString());
				Phone_Result result = new Phone_Result();
				result.ip_addr = packet.getAddress();
				result.gsm_bit_error_rate = 0;
				result.gsm_signal_strength = 0;
				Result_List.add(result);
				Log.d("RESULT LIST", "ADDED TO RESULT LIST");
			}
		} catch (SocketTimeoutException e) {
			Log.d(TAG, "Receive timed out");
		}
	}
	
	private void listenForBroadcasts(DatagramSocket socket) throws IOException {
		byte[] buf = new byte[1024];
		try {
			while (true) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				String s = new String(packet.getData(), 0, packet.getLength());
				Log.d(TAG, "Received request " + s);
				Log.d(TAG, "IP is " + packet.getAddress().toString());
				//Phone_Result result = new Phone_Result();
				//result.ip_addr = packet.getAddress();
				//Result_List.add(result);
				Log.d("PEER REQUEST", "Peer requesting information");
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
	
	
	//getting signal strength is a bit of a pain,
	//have to extend PhoneStateListener then use context and telephony manager
	//to listen. Apparently there may be another way to do this, but the vast
	//majority of people use this way: you can only get the signalstrength
	//when it is changed. Not ideal by any means, but hopefully this happens
	//fairly often on a lower level...
	public class myPhoneStateListener extends PhoneStateListener {
		
		int signal_strength;
		int signal_err_rate;
		
		public myPhoneStateListener(){
			signal_strength = 0;
		}
		@Override
	    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
	        super.onSignalStrengthsChanged(signalStrength);
	        signal_strength = signalStrength.getGsmSignalStrength();
	        signal_err_rate = signalStrength.getGsmBitErrorRate();
		}
	}
	
	//#Method for peer request
	//This is really annoying, maybe you know more about accessing the context from Activity class,
	//I'm only guessing that this will work as of now, but let's hope that the context I pass
	//in the constructor is correct
	public Phone_Result peer_request() {
		Phone_Result res = new Phone_Result();
		//res.gsm_bit_error_rate = SignalStrength.getGsmBitErrorRate();
		//res.gsm_signal_strength = getGsmSignalStrength();
		//context = context.getApplicationContext();//getApplicationContext();
		TelephonyManager tel;
        myPhoneStateListener myListener;
        myListener = new myPhoneStateListener();
        tel = ( TelephonyManager )context.getSystemService(Context.TELEPHONY_SERVICE);
        while (myListener.signal_strength == 0) {
        	tel.listen(myListener ,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }
        tel.listen(myListener ,PhoneStateListener.LISTEN_NONE);
        res.gsm_signal_strength = myListener.signal_strength;
        res.gsm_bit_error_rate = myListener.signal_err_rate;
		return res;
	}
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
		public String reqTypeStr;
		public SerializableTypes typeEnum;
		public LatLng requestedTileTopLeft;
		public Request(String typeStr, SerializableTypes type) {
			reqTypeStr = typeStr;
			typeEnum = type;
		}
		public Request(String typeStr, SerializableTypes type, LatLng _requestedTileTopLeft) {
			this(typeStr,type);
			requestedTileTopLeft = _requestedTileTopLeft;
		}
		public byte[] toByteArray() {
			byte[] byteArr = new byte[2 + reqTypeStr.getBytes().length];
			byteArr[0] = (byte)typeEnum.ordinal();
			byteArr[1] = (byte)reqTypeStr.getBytes().length;
			System.arraycopy(reqTypeStr.getBytes(), 0, byteArr, 2, reqTypeStr.getBytes().length);
			// TODO Auto-generated method stub
			return byteArr;
		}
		public String toString() {
			return "Request(" + typeEnum + "): " + reqTypeStr;
		}
	}
	
	public Request requestFromByteArray(byte[] data) {
		// TODO Auto-generated method stub
		SerializableTypes t = SerializableTypes.values()[(int)data[0]];
		byte[] strbuf = new byte[(int)data[1]];
		
		System.arraycopy(data, 2, strbuf, 0, (int)data[1]);
		//strbuf[(int)data[1] - 1] = '\0';
		String s = new String(strbuf);
		switch (t) {
			case REQUEST_T:
				return new Request(s,t);
			case TILE_REQUEST_T:
			// TODO: unserialize the tile latlng here
				return null;
		}
		return new Request(s,t);
	}
	
	public class Phone_Result {
		public InetAddress ip_addr;
		int gsm_signal_strength;
		int gsm_bit_error_rate;
		int secondsAlive;
		int numCachedSegments;
	}
}
