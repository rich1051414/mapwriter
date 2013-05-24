package mapwriter.map;

import java.util.ArrayList;

import mapwriter.Mw;
import mapwriter.MwUtil;
import mapwriter.forge.MwConfig;
import mapwriter.map.mapmode.MapMode;

public class MarkerManager {
	
	private Mw mw;
	
	public ArrayList<Marker> markerList = new ArrayList<Marker>();
	public ArrayList<String> groupList = new ArrayList<String>();
	
	public ArrayList<Marker> visibleMarkerList = new ArrayList<Marker>();
	
	private String visibleGroupName = "none";
	
	public Marker selectedMarker = null;
	
	public MarkerManager(Mw mw) {
		this.mw = mw;
	}
	
	public void load(MwConfig config, String category) {
		this.markerList.clear();
		
		if (config.hasCategory(category)) {
			int markerCount = config.get(category, "markerCount", 0).getInt();
			this.visibleGroupName = config.get(category, "visibleGroup", "").getString();
			
			if (markerCount > 0) {
				for (int i = 0; i < markerCount; i++) {
					String key = "marker" + i;
					String value = config.get(category, key, "").getString();
					Marker marker = this.stringToMarker(value);
					if (marker != null) {
						this.addMarker(marker);
					} else {
						MwUtil.log("error: could not load " + key + " from config file");
					}
				}
			}
		}
		
		this.update();
	}
	
	public void save(MwConfig config, String category) {
		config.get(category, "markerCount", 0).set(this.markerList.size());
		config.get(category, "visibleGroup", "").set(this.visibleGroupName);
		
		int i = 0;
		for (Marker marker : this.markerList) {
			String key = "marker" + i;
			String value = this.markerToString(marker);
			config.get(category, key, "").set(value);
			i++;
		}
	}
	
	public void setVisibleGroupName(String groupName) {
		if (groupName != null) {
			this.visibleGroupName = MwUtil.mungeString(groupName);
		} else {
			this.visibleGroupName = "none";
		}
	}
	
	public String getVisibleGroupName() {
		return this.visibleGroupName;
	}
	
	public void clear() {
		this.markerList.clear();
		this.groupList.clear();
		this.visibleMarkerList.clear();
		this.visibleGroupName = "none";
	}
	
	public String markerToString(Marker marker) {
		return String.format("%s %d %d %d %06x %s",
				marker.name,
				marker.x, marker.y, marker.z,
				marker.colour & 0xffffff,
				marker.groupName);
	}
	
	public Marker stringToMarker(String s) {
		String[] split = s.split(" ");
		Marker marker = null;
		if (split.length == 6) {
			try {
				int x = Integer.parseInt(split[1]);
				int y = Integer.parseInt(split[2]);
				int z = Integer.parseInt(split[3]);
				int colour = 0xff000000 | Integer.parseInt(split[4], 16);
				
				marker = new Marker(split[0], split[5], x, y, z, colour);
				
			} catch (NumberFormatException e) {
				marker = null;
			}
		} else {
			MwUtil.log("Marker.stringToMarker: incorrect number of parameters %d, need 6", split.length);
		}
		return marker;
	}
	
	public void addMarker(Marker marker) {
		this.markerList.add(marker);
	}
	
	public void addMarker(String name, String groupName, int x, int y, int z, int colour) {
		name = MwUtil.mungeString(name);
		groupName = MwUtil.mungeString(groupName);
		this.addMarker(new Marker(name, groupName, x, y, z, colour));
	}
	
	// returns true when marker does not exist in map
	public boolean delMarker(Marker markerToDelete) {
		return this.markerList.remove(markerToDelete);
	}
	
	public boolean delMarker(String name, String group) {
		Marker markerToDelete = null;
		for (Marker marker : this.markerList) {
			if (marker.name.equals(name) && marker.groupName.equals(group)) {
				markerToDelete = marker;
			}
		}
		return this.delMarker(markerToDelete);
	}
	
	/*public boolean delGroup(String groupName) {
		boolean error = !this.groupList.remove(groupName);
	    Iterator it = this.markerMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry entry = (Map.Entry) it.next();
	        Marker marker = (Marker) entry.getValue();
	        if (marker.groupName.equals(groupName)) {
	        	it.remove();
	        }
	    }
	    if (groupName == this.visibleGroupName) {
			this.nextGroup();
		}
	    return error;
	}*/
	
	public void update() {
		this.visibleMarkerList.clear();
		this.groupList.clear();
		this.groupList.add("none");
		this.groupList.add("all");
		for (Marker marker : this.markerList) {
			if (marker.groupName.equals(this.visibleGroupName) || this.visibleGroupName.equals("all")) {
				this.visibleMarkerList.add(marker);
			}
			if (!this.groupList.contains(marker.groupName)) {
				this.groupList.add(marker.groupName);
			}
		}
		if (!this.groupList.contains(this.visibleGroupName)) {
			this.visibleGroupName = "none";
		}
	}
	
	public void nextGroup(int n) {
		if (this.groupList.size() > 0) {
			int i = this.groupList.indexOf(this.visibleGroupName);
			int size = this.groupList.size();
			if (i != -1) {
				i = (i + size + n) % size;
			} else {
				i = 0;
			}
			this.visibleGroupName = this.groupList.get(i);
		} else {
			this.visibleGroupName = "none";
			this.groupList.add("none");
		}
	}
	
	public void nextGroup() {
		this.nextGroup(1);
	}
	
	public int countMarkersInGroup(String group) {
		int count = 0;
		if (group.equals("all")) {
			count = this.markerList.size();
		} else {
			for (Marker marker : this.markerList) {
				if (marker.groupName.equals(group)) {
					count++;
				}
			}
		}
		return count;
	}
	
	public void selectNextMarker() {
		if (this.visibleMarkerList.size() > 0) {
        	int i = 0;
        	if (this.selectedMarker != null) {
        		i = this.visibleMarkerList.indexOf(this.selectedMarker);
        		if (i == -1) {
        			i = 0;
        		}
        	}
        	i = (i + 1) % this.visibleMarkerList.size();
        	this.selectedMarker = this.visibleMarkerList.get(i);
    	} else {
    		this.selectedMarker = null;
    	}
	}
	
	public Marker getNearestMarker(int x, int z, int maxDistance) {
		int nearestDistance = maxDistance * maxDistance;
		Marker nearestMarker = null;
		for (Marker marker : this.visibleMarkerList) {
			int dx = x - marker.x;
			int dz = z - marker.z;
			int d = (dx * dx) + (dz * dz);
			if (d < nearestDistance) {
				nearestMarker = marker;
				nearestDistance = d;
			}
		}
		return nearestMarker;
	}
	
	public Marker getNearestMarkerInDirection(int x, int z, double desiredAngle) {
		int nearestDistance = 10000 * 10000;
		Marker nearestMarker = null;
		for (Marker marker : this.visibleMarkerList) {
			int dx = marker.x - x;
			int dz = marker.z - z;
			int d = (dx * dx) + (dz * dz);
			double angle = Math.atan2(dz, dx);
			if ((Math.cos(desiredAngle - angle) > 0.8D) && (d < nearestDistance) && (d > 4)) {
				nearestMarker = marker;
				nearestDistance = d;
			}
		}
		return nearestMarker;
	}
	
	public void drawMarkers(MapMode mapMode, MapView mapView) {
		for (Marker marker : this.visibleMarkerList) {
			marker.draw(mapMode, mapView, 0xff000000);
		}
		if (this.selectedMarker != null) {
			this.selectedMarker.draw(mapMode, mapView, 0xffffffff);
		}
	}
}
