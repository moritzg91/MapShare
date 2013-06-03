package com.northwestern.mapshare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.app.FragmentManager;

import android.view.View;
import android.widget.EditText;
import android.widget.Button;

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
    		
			switch (m_NETWORKING_MODE) {
	    	case TRADITIONAL_3G_OR_WIFI:
	    		for (int idx = 0; idx < addrList.size(); idx++) {
	    			m_gMap.addMarker(new MarkerOptions()
	            		.position(new LatLng(addrList.get(idx).getLatitude(), addrList.get(idx).getLongitude()))
	            		.title(Integer.toString(idx)));
	    		}
	    		// scroll camera to first marker
	    		 m_gMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(addrList.get(0).getLatitude(),addrList.get(0).getLongitude())));
	    		 this.cacheView();
	    		 return;
	    	case PSEUDOCAST_AND_3G:
	    		List<MapSegment> segmentsToRequest = this.initSegmentList(addrList);
	    		List<Result> aggregateResults = new ArrayList<Result>();
	    		for (int idx = 0; idx < addrList.size(); idx++) {
	    			List<Result> results = m_broadcastMngr.requestSegments(segmentsToRequest.get(idx));
	    			aggregateResults.addAll(results);
	    		}
	    		this.renderMap(aggregateResults);
	    		return;
	    	case PSEUDOCAST_CACHE_ONLY:
	    		break;
	    	}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void renderMap(List<Result> aggregateResults) {
		// TODO Auto-generated method stub
		
	}
	// based on the address list, find which addresses are already in the cache and which ones we need to request. Return the list of those we need to request.
    private List<MapSegment> initSegmentList(List<Address> addrList) {
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
}

