package com.northwestern.mapshare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.location.*;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.northwestern.mapshare.BroadcastManager.Result;
import com.northwestern.mapshare.Scheduler.MapSegment;

public class MainActivity extends Activity {
	public enum NetworkingMode {
		TRADITIONAL_3G_OR_WIFI, // just download over 3G
		PSEUDOCAST_AND_3G, // actively schedule DL on other phones
		PSEUDOCAST_CACHE_ONLY // just get data that other phones have in cache, but don't initiate downloads on other phones
	};

	private MapFragment m_mapFragment;
	private GoogleMap m_gMap;
	private EditText m_searchBar;
	private Button m_searchSubmitBtn;
	private Geocoder m_geocoder;
	private BroadcastManager m_broadcastMngr;

	private NetworkingMode m_NETWORKING_MODE;
	
	private final Context m_context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // manually set the networking mode here.
        m_NETWORKING_MODE = NetworkingMode.TRADITIONAL_3G_OR_WIFI;

        m_broadcastMngr = new BroadcastManager();

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
    }
 // perform the search
    protected void performSearch(String query) {
    	// get location from search string
    	try { //getFromLocationName throws exceptions, so we need the try/catch block
    		List<Address> addrList = m_geocoder.getFromLocationName(query, 10);
    		letUserPickAddress(addrList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    //we display every address, as a button maybe? Then user just hits
    //whichever choice they want
	protected void letUserPickAddress(List<Address> addrList)
	{
		LinearLayout buttonLayout = (LinearLayout)findViewById(R.id.btnlayout);
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

	private void handleAddressSelected(Address chosenAddr) {
		switch (m_NETWORKING_MODE) {
    	case TRADITIONAL_3G_OR_WIFI:
    		m_gMap.addMarker(new MarkerOptions()
            	.position(new LatLng(chosenAddr.getLatitude(), chosenAddr.getLongitude()))
            		.title(chosenAddr.toString()));
    		// scroll camera to first marker
    		 m_gMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(chosenAddr.getLatitude(),chosenAddr.getLongitude())));
    		 this.cacheView();
    		 return;
    	case PSEUDOCAST_AND_3G:
    		List<MapSegment> segmentsToRequest = this.initSegmentList(chosenAddr);
    		List<Result> results = m_broadcastMngr.requestSegments(segmentsToRequest);

    		this.renderMap(results);

    		return;
    	case PSEUDOCAST_CACHE_ONLY:
    		break;
    	}
	}
	
    private void renderMap(List<Result> aggregateResults) {
		// TODO Auto-generated method stub

	}
	// based on the address list, find which addresses are already in the cache and which ones we need to request. Return the list of those we need to request.
    private List<MapSegment> initSegmentList(Address chosenAddr) {
		// TODO Auto-generated method stub
		return null;
	}
	// cache the currently visible view
    protected void cacheView() {
    	CameraPosition camPosn = m_gMap.getCameraPosition();
    	LatLng coords = camPosn.target;
    	float zoomLvl = camPosn.zoom;

    	String img_id = Double.toString(coords.latitude) + "-" + Double.toString(coords.longitude) + "-" + Float.toString(zoomLvl);

    	Scraper.scrapeScreen(findViewById(R.id.map_fragment),img_id);
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
        	m_parentDialog.dismiss();
            handleAddressSelected(m_addrs.get(position));
        }
    }
}

