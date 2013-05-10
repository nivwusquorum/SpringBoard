package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class ThirdLocationBasedMessageExchange extends NakMessageProtocol {
	private static final double DIFFUSE_DISTANCE = 30;
	private static final int DELAY_DIVIDEDBY_16 = 3; // 
	private class DelayedLocation {
		Point [] buffer;
		int last;
		public DelayedLocation(int delay) {
			buffer = new Point[delay+1];
			last = 0;
		}
		
		public void updateLocation(Point l) {
			buffer[last++] = l;
			if (last == buffer.length) last = 0;
		}
		
		public Point getDelayedLocation() {
			int next = last+1;
			if (next == buffer.length) next = 0;
			return buffer[next];
		}
	}
	
	Map<Integer, DelayedLocation> dl;
	
	private DelayedLocation getDl(int id) {
		DelayedLocation l = dl.get(id);
		if (l == null) {
			dl.put(id, l= new DelayedLocation(DELAY_DIVIDEDBY_16));
		}
		return l;
	}
	
	private Point diffuse(Point p) {
		double angle = Math.toRadians(Math.random() * 360);
		int dx = (int)(DIFFUSE_DISTANCE * Math.cos(angle));
		int dy = (int)(DIFFUSE_DISTANCE * Math.sin(angle));
		return new Point(p.getX() + dx, p.getY() + dy);
	}
	
	Map<Integer, Point> messageLocation;
	
	
	public ThirdLocationBasedMessageExchange() {
		super();
		messageLocation = new HashMap<Integer, Point>();
		dl = new HashMap<Integer, DelayedLocation>();
	}
	
	private Double maxDistance = null;
	
	@Override
	protected double getProbabilityOfDelivery(int msg, SpringBoardUser from,
			SpringBoardUser to) {
		double prev = super.getProbabilityOfDelivery(msg, from, to);
		boolean DEBUG = false; // r.nextInt(1000) == 0;
		if(DEBUG && prev == 0.0)
			System.out.println("prev(0)");

		if (prev == 1.0) return 1.0; // those from bloom filter;
		if (prev == 0.0) return 0.0;
		assert prev > 0.0;
		if (maxDistance == null) {
			Dimension m = SpringBoardUser.getModelDims();
			if (m == null) return prev;
			else {
				double dx2 = m.getWidth()*m.getWidth();
				double dy2 = m.getHeight()*m.getHeight();
				// add 1 to avoid division by 0 error.
				maxDistance = Math.sqrt(dx2+dy2)+1;
			}
		}
		Point delayed = getDl(to.getID()).getDelayedLocation();
		
		Point a = (delayed == null) ? to.getLocation() : delayed;
		Point b = messageLocation.get(msg);
		assert b != null;
		double curDistance = Math.sqrt(Tools.pointsDistanceSquared(a,b));
		
		// System.out.println("cur: " + curDistance + ", max: " + maxDistance);
		double locationBasedProbability = Math.max(2*maxDistance/3-curDistance,0)/(2*maxDistance/3);

		assert 0.0 <= locationBasedProbability && locationBasedProbability <= 1.0;
		double curve = (1.0 - Math.pow(1.0-locationBasedProbability,2));
		
		locationBasedProbability = 0.8*curve+0.2;
		// expected value of location based probability is about 0.1
		double ret = prev*locationBasedProbability;
		if (DEBUG)
			System.out.println("prev(" + prev + ") -> prob(" + ret+") when dist= " +curDistance);
		return ret;
		
	}
	

	@Override
	public void messageCreated(Integer mId, SpringBoardUser to) {
		messageLocation.put(mId, diffuse(getCharacteristicLocation(to)));
	}
	
	@Override
	public void step() {
		super.step();
		if ((RealisticModel.getStepsExecuted()&15) != 0) return;
		RealisticModel m = RealisticModel.getLatestInstance();
		if ( m == null ) return;
		for (Integer i : m.getUsers().keySet()) {
			User u = m.getUsers().get(i);
			updateCurrentLocation(u.getID(), u.getLocation());
			
		}
	}

	protected void updateCurrentLocation(int id, Point location) {
		getDl(id).updateLocation(location);
		
	}

	protected Point getCharacteristicLocation(User u) {
		return u.getLocation();
	}
	
	
}
