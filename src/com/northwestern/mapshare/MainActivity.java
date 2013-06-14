package com.northwestern.mapshare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ListView;
import android.net.wifi.*;

import android.location.*;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.northwestern.mapshare.BroadcastManager.Phone_Result;
import com.northwestern.mapshare.BroadcastManager.Request;
import com.northwestern.mapshare.BroadcastManager.Result;
import com.northwestern.mapshare.BroadcastManager.SerializableTypes;

public class MainActivity extends Activity {
	
	protected final long EARTH_CIRCUMFERENCE = 40075000; //meters
	protected long EARTH_LAT_CIRCUMFERENCE[];
	
	public enum NetworkingMode {
		TRADITIONAL_3G_OR_WIFI, // just download over 3G
		PSEUDOCAST_AND_3G, // actively schedule DL on other phones
		BROADCAST,
		PSEUDOCAST_CACHE_ONLY // just get data that other phones have in cache, but don't initiate downloads on other phones
	};

	private MapFragment m_mapFragment;
	private GoogleMap m_gMap;
	private EditText m_searchBar;
	private Button m_searchSubmitBtn;
	private Geocoder m_geocoder;
	private BroadcastManager m_broadcastMngr;
	private WifiManager m_wifiMngr;
	private View m_mapRenderView;

	private NetworkingMode m_NETWORKING_MODE;
	
	private final Context m_context = this;
	private Thread m_cacheViewThread;
	public List<Result> m_cachedTiles;
	
	private Map<Integer,Phone_Result> m_peers;
	
	public final float MATCHING_OFFSET_DELTA = 0.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // manually set the networking mode here.
        m_NETWORKING_MODE = NetworkingMode.TRADITIONAL_3G_OR_WIFI;
        //setup wifi
        m_wifiMngr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        
        //Not sure if this will work, but we need the current context to access signal strengths
        //if all goes well, each phone's BroadcastManager will have the correct context
        //and thus transmit the correct data back in the response to the request
        Context c = (Context) this;
        //get wifi status
        WifiInfo info = m_wifiMngr.getConnectionInfo();
        Log.d("WifiManager", "WiFi Status: " + info.toString());
        
        //Create Broadcast Manager and run thread
        m_broadcastMngr = new BroadcastManager(m_wifiMngr,c,this);//.start();
        m_broadcastMngr.start();
        
        //Use latch to wait so that we don't start doing map segment requests
        //before we have list of phones. waiting does this.
        /*
        try {
			m_broadcastMngr.waiting_one();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        
        try {
			m_broadcastMngr.waiting_one();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        Log.d("LENGTH OF RESULTS", "length is: " + m_broadcastMngr.Result_List.size());
        m_peers = ratePeers(m_broadcastMngr.Result_List);
        Log.d("Rated Peers", "Size of map is " + m_peers.size());
        //m_broadcastMngr.listen_for_broadcasts();
        // initialize the references to UI elements
        FragmentManager fm = getFragmentManager();
        m_mapFragment = (MapFragment)fm.findFragmentById(R.id.map_fragment);
        m_gMap = m_mapFragment.getMap();

        m_searchBar = (EditText)findViewById(R.id.searchBar);
        m_searchSubmitBtn = (Button)findViewById(R.id.searchSubmitBtn);

        // initialize geocoder
        m_geocoder = new Geocoder(this);

        // set callback for search submit button
        m_searchSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	System.out.println("clicked");
                performSearch(m_searchBar.getText().toString());
            }
        });
        
        m_mapRenderView = null;
        m_cachedTiles = new LinkedList<Result>();
        
        EARTH_LAT_CIRCUMFERENCE = new long[180];
        for (int i=0;i<90;i++) {
        	long circumference = (long) (2*Math.PI*6378000*Math.cos(i));
        	EARTH_LAT_CIRCUMFERENCE[i] = circumference;
        	EARTH_LAT_CIRCUMFERENCE[i+90] = circumference;
        }
    }
    private Map<Integer,Phone_Result> ratePeers(List<Phone_Result> result_List) {
    	Map<Integer, Phone_Result> ratedPeers = new HashMap<Integer, Phone_Result>();
    	for (Phone_Result node : result_List) {
    		if (node.gsm_bit_error_rate != 0)
    			ratedPeers.put((int) (node.numCachedSegments + Math.log(node.secondsAlive) + node.gsm_signal_strength/node.gsm_bit_error_rate), node);
    	}
		// TODO Auto-generated method stub
		return ratedPeers;
	}
	// perform the search
    protected void performSearch(String query) {
    	// get location from search string
    	try { //getFromLocationName throws exceptions, so we need the try/catch block
    		List<Address> addrList = m_geocoder.getFromLocationName(query, 10);
    		letUserPickAddress(addrList);
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
    }
    //we display every address, as a button maybe? Then user just hits
    //whichever choice they want
	protected void letUserPickAddress(List<Address> addrList)
	{
		ListView modeList = new ListView(m_context);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(m_context);
		
		int list_len = addrList.size();
		String[] addresses = new String[list_len];
		
		for (int i = 0; i < list_len; i++) { 
			addresses[i] = addrList.get(i).getAddressLine(0) + "\n" + addrList.get(i).getAddressLine(1) + "\n" + addrList.get(i).getAddressLine(2); 
		}
		
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, addresses);
		modeList.setAdapter(modeAdapter);
		
		alertDialogBuilder.setView(modeList);
		
		AlertDialog alertDialog = alertDialogBuilder.create();
		modeList.setOnItemClickListener(new CustomOnItemClickListener(addrList,alertDialog));
		// show dialog
		alertDialog.show();
	}

	private void handleAddressSelected(AlertDialog dialog, Address chosenAddr) {
		float zoomLvl;
		CameraPosition camPosn;
		switch (m_NETWORKING_MODE) {
		case BROADCAST:
			//so now we have chosen address. Initiate download from rated peers
			List<Request> req_list = initSegmentList(chosenAddr, m_gMap.getCameraPosition().zoom);
			//now divide based on number of peers
			int num_for_each = 2; //req_list is size 4, so 2 for peer, 2 for us (hardcoded for now)
			//give 2 to peer
			m_broadcastMngr.requestSegments(req_list);
			//this means send request for download via BroadcastManager mBroadcastMngr
			//request is "TileRequest"
			
			break;
    	case TRADITIONAL_3G_OR_WIFI:
    		if (m_mapRenderView != null) {
    			m_mapRenderView = null;
    			// TODO: do we need to unload the view somehow?
    		}
    		m_gMap.addMarker(new MarkerOptions()
            	.position(new LatLng(chosenAddr.getLatitude(), chosenAddr.getLongitude()))
            	.title(chosenAddr.toString()));
    		// scroll camera to first marker
    		 m_gMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(chosenAddr.getLatitude(),chosenAddr.getLongitude())));


         	camPosn = m_gMap.getCameraPosition();
         	LatLng coords = camPosn.target;
         	zoomLvl = camPosn.zoom;

         	String img_id = android.util.Base64.encodeToString((Double.toString(coords.latitude) + "-" + Double.toString(coords.longitude) + "-" + Float.toString(zoomLvl)).getBytes(),android.util.Base64.DEFAULT);
    		 
    		 m_cacheViewThread = new Thread(new CacheView(img_id,coords,EARTH_LAT_CIRCUMFERENCE[(int)coords.latitude]/Math.pow(zoomLvl,2),EARTH_CIRCUMFERENCE/Math.pow(zoomLvl,2)));
             m_cacheViewThread.start();
    		 
    		 return;
    	case PSEUDOCAST_AND_3G:
    		camPosn = m_gMap.getCameraPosition();
         	//LatLng coords = camPosn.target;
         	zoomLvl = camPosn.zoom;
    		List<Request> segmentsToRequest = this.initSegmentList(chosenAddr,zoomLvl);
    		List<Result> results = m_broadcastMngr.requestSegments(segmentsToRequest);

    		this.renderMap(results);

    		return;
    	case PSEUDOCAST_CACHE_ONLY:
    		break;
    	}
	}
	
    private void renderMap(List<Result> aggregateResults) {
    	// sort aggregate results for merging
    	Collections.sort(aggregateResults, new ResultComparator());
    	List<Bitmap> map_bmps = new ArrayList<Bitmap>();
    	for (Result res : aggregateResults) {
    		map_bmps.add(res.map_image);
    	}
    	int span = (int) Math.sqrt(aggregateResults.size());
    	Bitmap joinedBmp = Helpers.combineBitmaps(map_bmps, span, span);
    	//LinearLayout root = (LinearLayout)findViewById(R.id.rootLayout);
    	// TODO: set the content of the rootView to joinedBmp

	}
	// based on the address list, find which addresses are already in the cache and which ones we need to request. Return the list of those we need to request.
    private List<Request> initSegmentList(Address chosenAddr, float zoomlvl) {
    	List<Request> segmentList = new LinkedList<Request>();
    	// calculate the tiles we need
    	LatLng center = new LatLng(chosenAddr.getLatitude(),chosenAddr.getLongitude());
    	// top left
    	segmentList.add( m_broadcastMngr.new Request("TileRequest",SerializableTypes.TILE_REQUEST_T,new LatLng(center.latitude - EARTH_LAT_CIRCUMFERENCE[(int)center.latitude]/Math.pow(zoomlvl,2),center.longitude + EARTH_CIRCUMFERENCE/Math.pow(zoomlvl,2))) );
    	// top right
    	segmentList.add( m_broadcastMngr.new Request("TileRequest",SerializableTypes.TILE_REQUEST_T,new LatLng(center.latitude,center.longitude + EARTH_CIRCUMFERENCE/Math.pow(zoomlvl,2))) );
    	// bottom left
    	segmentList.add( m_broadcastMngr.new Request("TileRequest",SerializableTypes.TILE_REQUEST_T,new LatLng(center.latitude - EARTH_LAT_CIRCUMFERENCE[(int)center.latitude]/Math.pow(zoomlvl,2),center.longitude)) );
    	// bottom right
    	segmentList.add( m_broadcastMngr.new Request("TileRequest",SerializableTypes.TILE_REQUEST_T,center) );
    	
		// TODO Auto-generated method stub
		return segmentList;
	}
    
    protected class CustomButton extends Button {
    	public Address myAddress;
		public CustomButton(Context context, Address referencingAddress) {
			super(context);
			myAddress = referencingAddress;
			// TODO Auto-generated constructor stub
		}
    }
    
    private class CustomOnItemClickListener implements OnItemClickListener {
    	List<Address> m_addrs;
    	AlertDialog m_parentDialog;
    	public CustomOnItemClickListener(List<Address> addrs, AlertDialog parentDialog) {
    		m_addrs = addrs;
    		m_parentDialog = parentDialog;
    	}
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	// hide keyboard
        	InputMethodManager imm = (InputMethodManager)getSystemService(
        		      Context.INPUT_METHOD_SERVICE);
        		imm.hideSoftInputFromWindow(m_searchBar.getWindowToken(), 0);
        	m_parentDialog.hide();
        	m_parentDialog.dismiss();
        	// handle address selection
            handleAddressSelected(m_parentDialog,m_addrs.get(position));
        }
    }
    
    public class ResultComparator implements Comparator<Result> {
	    @Override
	    public int compare(Result r1, Result r2) {
	        if (r1.topLeft.longitude > r2.topLeft.longitude) {
	        	return -1;
	        } else if (r1.topLeft.longitude == r2.topLeft.longitude) {
	        	if (r1.topLeft.latitude < r2.topLeft.latitude) {
		        	return -1;
		        } else if (r1.topLeft.latitude > r2.topLeft.latitude) {
		        	return 1;
		        } else {
		        	return 0;
		        }
	        }
	        return 1;
	    }
	}
    
    public class CacheView implements Runnable {
    	String m_imgName;
    	Result m_cacheResult;
    	public CacheView(String img_name, LatLng coords, double d, double e) {
    		super();
    		m_imgName = img_name;
    		m_cacheResult = m_broadcastMngr.new Result(coords,d,e,null);
    	}
    	@Override
    	public void run() {
    		System.out.println("running cacheview thread");
    		cacheView();
    	}
    	public void cacheView() {
    		System.out.println("starting cacheView()");
        	try {
    			Thread.sleep(1500);
    			System.out.println("running cache view");
    		} catch (InterruptedException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        	/* take a screenshot of the screen and crop out the UI elements that are not part of the map
        	 * (ugly hack, but hey, it's not my fault google maps is retarded) */
        	Scraper.scrapeScreen(findViewById(R.id.map_fragment),m_imgName);
        	// crop the screenshot
        	Bitmap bmp = BitmapFactory.decodeFile(m_imgName);
        	// TODO: do cropping
        	
        	// save bitmap in cache
        	m_cacheResult.map_image = bmp;
        	m_cachedTiles.add(m_cacheResult);
        }
    }
}

