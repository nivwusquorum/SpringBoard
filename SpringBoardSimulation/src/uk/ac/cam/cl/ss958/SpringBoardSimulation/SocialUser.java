package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sun.xml.internal.bind.v2.model.core.MaybeElement;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class SocialUser extends User {
	static AdvancedRandom generator =  new AdvancedRandom (System.currentTimeMillis());

	private static final int EXPECTED_TOTOAL_FRIENDS = 10;
	private static final int INV_PROBABILITY_ADDING_FRIEND = 50;
	
	private int maxFriends;
	
	private List<User> friends; 
	
	private boolean constructorDone = false;
	
	public SocialUser(SimulationModel mainModel)
			throws CannotPlaceUserException {
		super(mainModel);
		// TODO: This should be power distribution
		maxFriends = (int)generator.nextExponential(1.0/EXPECTED_TOTOAL_FRIENDS);
		friends = new ArrayList<User>();
		constructorDone = true;
	}

	public List<User> getFriends() {
		return friends;
	}
	
	@Override
	public void setLocation(Point location) throws CannotPlaceUserException {
		// TODO Auto-generated method stub
		super.setLocation(location);
		if (constructorDone)
			maybeMakeFriends();
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
	
}
