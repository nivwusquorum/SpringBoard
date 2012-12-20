package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import uk.ac.cam.cl.ss958.IntegerGeometry.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class SimulationModel {		
	enum SoftError {
		NONE,
		WRONG_MOVING;
	}


	final Random generator = new Random (System.currentTimeMillis());
	
	private int width;
	private int height;

	private int selectedUser;
	private Point selectedUserClickTranslation;
	private HashMap<Integer, User> users;
	
	private SoftError softError = SoftError.NONE;
	
	public boolean isSoftError() {
		return softError != SoftError.NONE;
	}
	
	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	SimulationModel(int width, int height) {
		this.width = width;
		this.height = height;
		users = new HashMap<Integer, User>();
		selectedUser = -1;
		onChange();
	}
	
	public HashMap<Integer, User> getUsers() {
		return users;
	}
	
	public boolean validatePosition(Point x, int excludingIndex) {
		if (x.getX() <= User.USER_RADIUS || 
		    x.getX() >= width-User.USER_RADIUS ||
		    x.getY() <= User.USER_RADIUS ||
		    x.getY() >= height - User.USER_RADIUS) return false;
		for (Integer id : users.keySet()) {
			if (id != excludingIndex &&
			        Compute.euclideanDistanceSquared(x, users.get(id).getLocation()) 
			        <= Compute.square(2*User.USER_RADIUS)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean AddRandomUser() {
		try {
			User u = new User(this);
			users.put(u.getID(), u);
			onChange();
			return true;
		} catch(CannotPlaceUserException e) {
			onChange();
			return false;
		}
	}
	
	public int getSelectedUser() {
		return selectedUser;
	}

	public void maybeSelectUser(Point p) {
		for(Integer id : users.keySet()) {
			if(Compute.euclideanDistanceSquared(p, users.get(id).getLocation()) <= 
					Compute.square(User.USER_RADIUS)) {
				selectedUserClickTranslation = users.get(id).getLocation().sub(p);
				selectedUser = id;
				onChange();
				return;
			}
		}
		selectedUser = -1;
		onChange();
	}
	
	// TODO: move validating to user
	public void maybeMoveUser(Point p) {
		if (selectedUser != -1) {
			try {
				users.get(selectedUser).setLocation(p.add(selectedUserClickTranslation));
				removeErrorIfPresent(SoftError.WRONG_MOVING);
			} catch(CannotPlaceUserException e) {
				softError = SoftError.WRONG_MOVING;
			} finally {
				onChange();
			}
		}
	}
	
	public void movingFinished() {
		removeErrorIfPresent(SoftError.WRONG_MOVING);
	}
	public void clearUsers() {
		users.clear();
		selectedUser = -1;
		onChange();
	}
	
	
	protected void onChange() {
	}
	
	private void removeErrorIfPresent(SoftError se) {
		if (softError == se) {
			softError = SoftError.NONE;
			onChange();
		}
	}

}
