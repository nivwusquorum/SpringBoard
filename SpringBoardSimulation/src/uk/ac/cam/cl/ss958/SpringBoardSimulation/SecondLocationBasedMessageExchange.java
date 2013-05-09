package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class SecondLocationBasedMessageExchange extends NakMessageProtocol {
	private static final double FORGETTING = 0.01;
	private static final double LOCATION_RATIO = 0.5;
	
	class DPoint {
		double x;
		double y;
		DPoint(double x,
			   double y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private DPoint multiply(DPoint p, double d) {
		return new DPoint(p.x*d, p.y*d);
	}
	
	private DPoint add(DPoint a, DPoint b) {
		return new DPoint(a.x + b.x, a.y + b.y);
	}
	
	class LocationFactor {
		DPoint myAvgLocation;
		// average location factor of people I contact.
		DPoint averageLFOthers;
		public LocationFactor() {
			myAvgLocation = new DPoint(0.0,0.0);
			averageLFOthers = new DPoint(0.0,0.0);
		}
		
		long lastLocationUpdate = 0;
		
		public void updateMyLocation(Point location) {
			if (lastLocationUpdate == RealisticModel.getStepsExecuted()) 
				return;
			lastLocationUpdate = RealisticModel.getStepsExecuted();
			myAvgLocation.x = myAvgLocation.x*(1.0-FORGETTING) + FORGETTING*location.getX();
			myAvgLocation.y = myAvgLocation.y*(1.0-FORGETTING) + FORGETTING*location.getY();
		}
		
		public void updateLFOthers(DPoint lf) {
			averageLFOthers.x = averageLFOthers.x*(1.0-FORGETTING) + FORGETTING*lf.x;
			averageLFOthers.y = averageLFOthers.y*(1.0-FORGETTING) + FORGETTING*lf.y;
		}
		
		public DPoint getFactor() {
			return add(multiply(myAvgLocation, LOCATION_RATIO),
					   multiply(averageLFOthers, (1.0-LOCATION_RATIO)));
		}
	}
	
	Map<Integer, LocationFactor> userLf;
	Map<Integer, DPoint> messageLf;
	
	
	public SecondLocationBasedMessageExchange() {
		super();
		userLf = new HashMap<Integer, LocationFactor>();
		messageLf = new HashMap<Integer, DPoint>();
	}
	
	private void updateLf(SpringBoardUser a, SpringBoardUser b) {
		LocationFactor lfa = userLf.get(a.getID());
		if(lfa == null){
			userLf.put(a.getID(), lfa = new LocationFactor());
		}
		
		LocationFactor lfb = userLf.get(b.getID());
		if(lfb == null){
			userLf.put(b.getID(), lfb = new LocationFactor());
		}
		lfa.updateMyLocation(a.getLocation());
		lfb.updateMyLocation(b.getLocation());
		lfa.updateLFOthers(lfb.getFactor());
		lfb.updateLFOthers(lfa.getFactor());
	}
	
	private double getProbabilityOfFactorDeliveryHeuristic(DPoint a, DPoint b) {
		Dimension m = SpringBoardUser.getModelDims();
		if (m == null) return 1.0;
		else {
			double dx = Math.abs(a.x-b.x);
			double dy = Math.abs(a.y-b.y);
			return (dx+dy)/(m.getWidth() + m.getHeight());
		}
	}
	
	@Override
	protected double getProbabilityOfDelivery(int msg, SpringBoardUser from,
			SpringBoardUser to) {
		DPoint lf = messageLf.get(msg);
		LocationFactor toLfGen = userLf.get(to.getID());
		if (lf == null || toLfGen == null) {
			return super.getProbabilityOfDelivery(msg, from, to);			
		} else {
			DPoint toLf = toLfGen.getFactor();
			double sofar = super.getProbabilityOfDelivery(msg, from, to);	
			if(sofar == 0) 
				return 0;
			double f = getProbabilityOfFactorDeliveryHeuristic(lf, toLf);
			// 1/f because we are computing inverse probability. 
			// 0.1 because E(f) = 0.1 so E(1/f) = 10.0.
			return sofar*f;
		}
	}
	
	@Override
	protected void sendMessages(SpringBoardUser from, SpringBoardUser to,
			int maxMessages) {
		// TODO Auto-generated method stub
		super.sendMessages(from, to, maxMessages);
		updateLf(from, to);
		
	}
	
	@Override
	public void messageCreated(Integer mId, SpringBoardUser to) {
		LocationFactor lf = userLf.get(to.getID());
		if (lf != null) {
			messageLf.put(mId, lf.getFactor());
		}
	}
	
	
}
