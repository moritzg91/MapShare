package com.northwestern.mapshare;

import java.util.List;
import java.util.Map;

public class Scheduler {
	public static Map<BroadcastManager.Node,List<MapSegment>> scheduleSegmentDL(MapSegment segment) {
		return null;
	}
	
	public class MapSegment {
		private List<MapSegment> m_subSegments;
		public final List<MapSegment> getSubsegs() { return m_subSegments; }
	}
}
