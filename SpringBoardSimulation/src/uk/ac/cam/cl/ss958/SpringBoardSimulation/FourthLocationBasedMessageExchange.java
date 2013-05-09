package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;


public class FourthLocationBasedMessageExchange extends
	ThirdLocationBasedMessageExchange {
	public static final int CLUSTER_SIZE = 50;
	
	private class CharacteristicLocation {
		private class LocationCluster {
			double sx;
			double sy;
			int samples;
			
			public Point getLocation() {
				return new Point ((int)(sx/samples), (int)(sy/samples));
			}
			
			public int getSamples() {
				return samples;
			}
			
			public boolean canInclude(Point x) {
				Point clusterPoint = getLocation();
				if (Tools.pointsDistanceSquared(x, clusterPoint) <=
						CLUSTER_SIZE*CLUSTER_SIZE) {
					sx+=x.getX();
					sy+=x.getY();
					++samples;
					return true;
				} else {
					return false;
				}
			}
			
			public LocationCluster(Point x) {
				sx = x.getX();
				sy = x.getY();
				samples = 1;
			}
		}
		
		List<LocationCluster> clusters;
		
		public CharacteristicLocation() {
			clusters = new ArrayList<LocationCluster>();
		}
		
		public void addLocation(Point x) {
			int targetCluster = -1;
			for (int i=0; i<clusters.size(); ++i) {
				if (clusters.get(i).canInclude(x)) {
					targetCluster = i;
					break;
				}
			}
			if (targetCluster == -1) {
				clusters.add(new LocationCluster(x));
			} else {
				int i = targetCluster;
				while(i>0 && clusters.get(i-1).getSamples() <
							clusters.get(i).getSamples()) {
					LocationCluster temp = clusters.get(i-1);
					clusters.set(i-1, clusters.get(i));
					clusters.set(i, temp);
				}
			}
		}
		
		public Point getBestLocation() {
			if (clusters.size() == 0) {
				return null;
			} else {
				return clusters.get(0).getLocation();
			}
		}
	}
	
	Map<Integer, CharacteristicLocation> cl;
	
	public FourthLocationBasedMessageExchange() {
		cl = new HashMap<Integer, CharacteristicLocation>();
	}
	
	private CharacteristicLocation getCl(int x) {
		CharacteristicLocation l = cl.get(x);
		if (l == null) {
			cl.put(x, l = new CharacteristicLocation());
		}
		return l;
	}
	
	@Override
	protected Point getCharacteristicLocation(User u) {
		Point l = getCl(u.getID()).getBestLocation();
		if (RealisticModel.getLatestInstance().getSelectedUser() == u.getID() && l != null) {
			System.out.println("Best Location for selected user: "+ l.getX() + " " +l.getY());
		}
		return l == null ? u.getLocation() : l;
	}
	
	@Override
	protected void updateCurrentLocation(int id, Point location) {
		super.updateCurrentLocation(id, location);
		getCl(id).addLocation(location);
		
		// TODO Auto-generated method stub
	}

}
