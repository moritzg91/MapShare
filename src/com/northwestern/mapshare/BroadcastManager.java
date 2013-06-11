package com.northwestern.mapshare;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.northwestern.mapshare.BroadcastManager.Node;
import com.northwestern.mapshare.Scheduler.MapSegment;

public class BroadcastManager {
	public List<Result> requestSegments(List<MapSegment> segments) {
		return null;
	}
	
	public class Result {
		LatLng topLeft;
		int width;
		int height;
		Bitmap map_image;
	}
	public class Request {
		
	}
	public class Node {

		public void requestSegment(MapSegment mapSegment) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
