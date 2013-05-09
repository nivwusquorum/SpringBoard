package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class LocationBasedMessageExchange extends NakMessageProtocol {
	private static final double DIFFUSE_DISTANCE = 50;

	private Point diffuse(Point p) {
		double angle = Math.toRadians(Math.random() * 360);
		int dx = (int)(DIFFUSE_DISTANCE * Math.cos(angle));
		int dy = (int)(DIFFUSE_DISTANCE * Math.sin(angle));
		return new Point(p.getX() + dx, p.getY() + dy);
	}
	
	Map<Integer, Point> messageLocation;
	
	
	public LocationBasedMessageExchange() {
		super();
		messageLocation = new HashMap<Integer, Point>();
	}
	
	private Double maxDistance = null;
	
	@Override
	protected double getProbabilityOfDelivery(int msg, SpringBoardUser from,
			SpringBoardUser to) {
		double prev = super.getProbabilityOfDelivery(msg, from, to);
		boolean DEBUG = false; 
						//r.nextInt(1000) == 0;
		if(DEBUG && prev == 0.0)
			System.out.println("prev(0)");

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
		Point a = to.getLocation();
		Point b = messageLocation.get(msg);
		assert b != null;
		double curDistance = Math.sqrt(Tools.pointsDistanceSquared(a,b));
		curDistance = Math.max(0, curDistance-DIFFUSE_DISTANCE);
			
		// System.out.println("cur: " + curDistance + ", max: " + maxDistance);
		double locationBasedProbability = (maxDistance-curDistance)/maxDistance;
		// 0.9*(1-(1-x)^2)+0.1
		locationBasedProbability = 0.9*(1.0-Math.pow(1-locationBasedProbability, 1.2))+0.1;
		// expected value of location based probability is about 0.1
		double ret = prev*locationBasedProbability;
		if (DEBUG)
			System.out.println("prev(" + prev + ") -> prob(" + ret+") when dist= " +curDistance);
		return ret;
		
	}
	

	@Override
	public void messageCreated(Integer mId, SpringBoardUser to) {
		messageLocation.put(mId, diffuse(to.getLocation()));
	}
	
	
}
