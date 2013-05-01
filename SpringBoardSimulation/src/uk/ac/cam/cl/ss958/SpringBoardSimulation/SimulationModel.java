package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import uk.ac.cam.cl.ss958.IntegerGeometry.*;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

public abstract class SimulationModel {		
	
	private static final boolean CHECK_OVERLAP = false;
	enum SoftError {
		NONE,
		WRONG_MOVING;
	}
	
	protected int width;
	protected int height;
	
	protected int numSectorsW;
	protected int numSectorsH;
	protected int sectorW;
	protected int sectorH;

	protected int selectedUser;
	protected Point selectedUserClickTranslation;
	
	protected UserSet sector[][];
	
	protected SoftError softError = SoftError.NONE;
	
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
	
	private boolean drawRanges = true;
	public boolean drawRanges() {
		return drawRanges;
	}
	
	public void setDrawRanges(boolean v) {
		drawRanges = v;
		onChange();
	}
	
	public int division_ceiling(int x, int y) {
		return (x+y-1)/y;
	}
	
	SimulationModel(int width, int height) {
		this.width = width;
		this.height = height;
		selectedUser = -1;
		sectorW = 2*User.MAX_RANGE;
		sectorH = 2*User.MAX_RANGE;
		numSectorsW = division_ceiling(width, 2*User.MAX_RANGE);
		numSectorsH = division_ceiling(width, 2*User.MAX_RANGE);
		
		sector = new UserSet[numSectorsW][numSectorsH];
		for (int i=0; i<numSectorsW; ++i) {
			for(int j=0; j<numSectorsH; ++j) {
				sector[i][j] = new UserSet();
			}
		}
		
		onChange();
	}
	
	static Point [] sectorOffsets = new Point[] {
		new Point(0,0),
		new Point(1,0),
		new Point(-1,0),
		new Point(0,1),
		new Point(0,-1),
		new Point(1,1),
		new Point(1,-1),
		new Point(-1,1),
		new Point(-1,-1),
	};
	
	public Point locationToSector(Point l) {
		return  new Point(l.getX()/sectorW,
				l.getY()/sectorH);
	}
	
	public List<User> getNearbyUsers(User u) {
		List<User> ret = new ArrayList<User>();

		Point xSector = locationToSector(u.getLocation());
		
		for(int i=0; i <sectorOffsets.length; ++i) {
			Point currentSector = xSector.add(sectorOffsets[i]);
			if (currentSector.getX() < 0 || currentSector.getX() >= numSectorsW ||
				    currentSector.getY() < 0 || currentSector.getY() >= numSectorsH) { 
				continue;
			}
			for (User user : sector[currentSector.getX()][currentSector.getY()]) {
				ret.add(user);
			}
		}
		
		return ret;
	}
	
	public boolean validatePosition(Point x, int excludingIndex) {
		return validatePosition(x, excludingIndex, CHECK_OVERLAP);
	}
	
	public boolean validatePosition(Point x,
			int excludingIndex,
			boolean checkOverlap) {
		if (x.getX() <= User.USER_RADIUS || 
		    x.getX() >= width-User.USER_RADIUS ||
		    x.getY() <= User.USER_RADIUS ||
		    x.getY() >= height - User.USER_RADIUS) return false;
		
		if (checkOverlap) {
			Point xSector = locationToSector(x);
			
			for(int i=0; i <sectorOffsets.length; ++i) {
				Point currentSector = xSector.add(sectorOffsets[i]);
				if (currentSector.getX() < 0 || currentSector.getX() >= numSectorsW ||
					    currentSector.getY() < 0 || currentSector.getY() >= numSectorsH) { 
					continue;
				}
				for (User u : sector[currentSector.getX()][currentSector.getY()]) {
					if (u.getID() != excludingIndex &&
					    Compute.euclideanDistanceSquared(x, u.getLocation()) 
					        <= Compute.square(2*User.USER_RADIUS)) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	public int getSelectedUser() {
		return selectedUser;
	}

	public void maybeSelectUser(Point p) {
		for(Integer id : getUsers().keySet()) {
			if(Compute.euclideanDistanceSquared(p, getUsers().get(id).getLocation()) <= 
					Compute.square(User.USER_RADIUS)) {
				selectedUserClickTranslation = getUsers().get(id).getLocation().sub(p);
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
				getUsers().get(selectedUser).setLocation(p.add(selectedUserClickTranslation));
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
		
	protected void removeErrorIfPresent(SoftError se) {
		if (softError == se) {
			softError = SoftError.NONE;
			onChange();
		}
	}

	protected void onChange() {}
	
	public void prepaint(Graphics g) {}
	
	public void postpaint(Graphics g) {}
	
	public void addToOptionsMenu(GlobalOptionsPanel o) { }
	
	public abstract boolean AddRandomUser();
	
	public abstract Map<Integer, ? extends User> getUsers();
	
	public abstract void clearUsers();
	
	public abstract void simulationStep();
	
	public abstract int getStepLengthMs();
	
	
	protected static class UserSet extends HashSet<User> {
		
	}


	public void removeFromSector(Point s, User user) {
		UserSet us = sector[s.getX()][s.getY()];
		us.remove(user);
	}

	public void updatePosition(User user) {
		Point oldSector = user.getSector();
		Point newSector = locationToSector(user.getLocation());
		
		if (Tools.pointsEqual(oldSector, newSector))
			return;
		
		if (oldSector != null)
			sector[oldSector.getX()][oldSector.getY()].remove(user);
		
		sector[newSector.getX()][newSector.getY()].add(user);
	}
}
