package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Graphics;
import java.util.Random;
import java.util.ArrayList;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class User implements Comparable<User> {
	public static final int SELECTION_BOLDNESS = 2;
	public static final int USER_RADIUS = 3;
	public static final int MIN_RANGE = 10;	
	public static final int MAX_RANGE = 16;

	
	protected final AdvancedRandom generator = 
			new AdvancedRandom (System.currentTimeMillis());
	
	static int numberOfIds = 0;
	
	public static int getNumerOfUsers() {
		return numberOfIds;
	}
	
	public static void resetCounter() {
		numberOfIds = 0;
	}
	
	protected int range;
	protected int id;
	protected SimulationModel model;
	private Point location;

	protected UserOptionsPanel optionsPanel;
	
	public void setOptionsPanel(UserOptionsPanel panel) {
		optionsPanel = panel;
	}
	
	
	public Point getLocation() {
		return location;
	}
	
	public int getID() {
		return id;
	}
	
	private Point sector;
	
	public Point getSector() { return sector; }
	
	public void setSector(Point s) {
		sector = s;
	}
	
	
	public void setLocation(Point location) throws CannotPlaceUserException {
		if(model.validatePosition(location, id)) {
			this.location = location;
			model.updatePosition(this);
		} else { 
			throw new CannotPlaceUserException();
		}
	}

	public int getRange() {
		return range;
	}
	
	public void setRange(int r) {
		range = r;
	}
	
	public void draw(Graphics g, boolean selected) {
		g.setColor(Colors.USER_COLOR);
		g.fillOval(location.getX() - USER_RADIUS, 
				   location.getY() - USER_RADIUS,
				   2*USER_RADIUS,
				   2*USER_RADIUS);
		if (selected) {
			g.setColor(Colors.SELECTED_USER_COLOR);
			for(int i=0; i<SELECTION_BOLDNESS; ++i) {
				g.drawOval(location.getX() - (USER_RADIUS-i), 
						location.getY() - (USER_RADIUS-i),
						2*(USER_RADIUS-i),
						2*(USER_RADIUS-i));
			}
		}
		g.setColor(Colors.RANGE_COLOR);
		if (model.drawRanges()) 
				g.drawOval(location.getX()- range, location.getY()- range, 2*range, 2*range);
	}
	
	public User(SimulationModel mainModel) throws CannotPlaceUserException {
		assert SELECTION_BOLDNESS < USER_RADIUS && 
			   USER_RADIUS < MIN_RANGE &&
			   MIN_RANGE < MAX_RANGE;
		
		model = mainModel;
		
		int retries = 1000;
		boolean successfullyPlaced = false;
		while(retries-- >= 0) {
			Point newLocation = new Point(generator.nextInt(model.getWidth()),
									      generator.nextInt(model.getHeight()));
			if(model.validatePosition(newLocation, -1, true)) {
				successfullyPlaced = true;
				setLocation(newLocation);
				break;
			}
		}
		if(!successfullyPlaced) throw new CannotPlaceUserException();
		
		id = numberOfIds++;
		// TODO: change to exponential distribution
		range = MIN_RANGE + generator.nextInt(MAX_RANGE-MIN_RANGE);
	}

	@Override
	public int compareTo(User o) {
		if (o == null) return -1;
		return ((Integer)this.getID()).compareTo(o.getID());
	}
}
