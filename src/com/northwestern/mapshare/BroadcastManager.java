package com.northwestern.mapshare;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.northwestern.mapshare.BroadcastManager.Node;
import com.northwestern.mapshare.Scheduler.MapSegment;

public class BroadcastManager {
	public List<Result> requestSegments(Scheduler.MapSegment segment) {
		Map<Node,List<Scheduler.MapSegment>> segmentSchedule = Scheduler.scheduleSegmentDL(segment);
		
		Iterator<Entry<Node, List<MapSegment>>> it = segmentSchedule.entrySet().iterator();
		
		List<Result> results = new ArrayList<Result>();
		
	    while (it.hasNext()) {
	        Entry<Node,List<MapSegment>> pair = (Entry<Node,List<MapSegment>>)it.next();
	        List<MapSegment> segments = pair.getValue();
	        Node targetNode = pair.getKey();
	        for (int i=0; i < segments.size(); i++) {
	        	// this needs to happen concurrently
	        	targetNode.requestSegment(segments.get(i));
	        }
	    }
	    // wait until results are returned
	    // TODO: probably have some sort of timeout condition here
	    while (results.size() < segment.getSubsegs().size()) {
	    	// wait...
	    }
		return results;
	}
	
	public class Result {
		
	}
	public class Request {
		
	}
	public class Node {

		public void requestSegment(MapSegment mapSegment) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
