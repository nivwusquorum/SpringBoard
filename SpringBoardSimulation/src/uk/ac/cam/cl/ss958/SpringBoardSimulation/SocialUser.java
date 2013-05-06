package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cern.jet.random.Zeta;
import cern.jet.random.engine.MersenneTwister;

import com.sun.xml.internal.bind.v2.model.core.MaybeElement;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class SocialUser extends User {
	static AdvancedRandom generator =  new AdvancedRandom (System.currentTimeMillis());

	private static final int EXPECTED_TOTAL_FRIENDS = 10;
	private static final int INV_PROBABILITY_ADDING_FRIEND = 50;
	
	private static final Zeta degreeDistribution =
			new Zeta(1.07, 0.0, new MersenneTwister((int)System.currentTimeMillis()));
	
	private int maxFriends;
	
	protected List<User> friends; 
		
	public SocialUser(SimulationModel mainModel)
			throws CannotPlaceUserException {
		super(mainModel);
		// TODO: This should be power distribution
		maxFriends = degreeDistribution.nextInt();
		//(int)generator.nextExponential(1.0/EXPECTED_TOTAL_FRIENDS);
		friends = new ArrayList<User>();
	}

	public List<User> getFriends() {
		return friends;
	}
	
	@Override
	public void setLocation(Point location) throws CannotPlaceUserException {
		// TODO Auto-generated method stub
		super.setLocation(location);
	}
	
	private int manhattanDistance(User a, User b) {
		return Math.abs(a.getLocation().getX() - b.getLocation().getX()) +
			   Math.abs(a.getLocation().getY() - b.getLocation().getY());
	}
	

		
	private boolean shouldAddFriend() {
		if (friends.size() >= maxFriends)
			return false;
		else
			return generator.nextInt(INV_PROBABILITY_ADDING_FRIEND) == 0;
	}
	
	private void maybeMakeFriends() {
		if(generator.nextInt(100) == 0) {			
			for (User u : model.getNearbyUsers(this)) {
				if (manhattanDistance(this, u) < 5*USER_RADIUS && shouldAddFriend() &&
					u.getID() != this.getID() && !friends.contains(u)) {
					if(u instanceof SocialUser) {
						friends.add(u);
						((SocialUser)u).friends.add(this);
					}
					
				}
			}
			
		}
	}
	
	public void step() {
		super.step();
		maybeMakeFriends();
	}
	
}
