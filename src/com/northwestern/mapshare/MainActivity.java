package com.northwestern.mapshare;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.app.FragmentManager;

import android.view.View;
import android.widget.EditText;
import android.widget.Button;

import android.location.*;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity {
	public enum NetworkingMode {
		TRADITIONAL_3G_OR_WIFI,
		PSEUDOCAST_AND_3G,
		PSEUDOCAST_CACHE_ONLY
	};
	
	private MapFragment m_mapFragment;
	private GoogleMap m_gMap;
	private EditText m_searchBar;
	private Button m_searchSubmitBtn;
	private Geocoder m_geocoder;
	
	private NetworkingMode m_NETWORKING_MODE;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // set the networking mode
        m_NETWORKING_MODE = NetworkingMode.TRADITIONAL_3G_OR_WIFI;
        
        // initialize the references to UI elements
        FragmentManager fm = getFragmentManager();
        m_mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
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
	    	case PSEUDOCAST_AND_3G:
	    		break;
	    	case PSEUDOCAST_CACHE_ONLY:
	    		break;
	    	}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}

