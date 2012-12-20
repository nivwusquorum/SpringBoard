package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Graphics;
import java.util.Random;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class User {
	private static final Random generator = new Random (System.currentTimeMillis());
	static int numberOfIds = 0;
	
	private int range;
	private int id;
	private SimulationModel model;
	private Point location;

	
	public static final int USER_RADIUS = 10;
	
	public Point getLocation() {
		return location;
	}
	
	public int getID() {
		return id;
	}

	public void setLocation(Point location) throws CannotPlaceUserException {
		if(model.validatePosition(location, id))
			this.location = location;
		else 
			throw new CannotPlaceUserException();
	}

	public int getRange() {
		return range;
	}
	
	public void setRange(int r) {
		range = r;
	}
	
	public void draw(Graphics g, boolean selected) {
		g.setColor(Colors.USER_COLOR);
		if (selected) 
			g.setColor(Colors.SELECTED_USER_COLOR);
		g.fillOval(location.getX() - USER_RADIUS, 
				   location.getY() - USER_RADIUS,
				   2*USER_RADIUS,
				   2*USER_RADIUS);
		g.setColor(Colors.RANGE_COLOR);
		g.drawOval(location.getX()- range, location.getY()- range, 2*range, 2*range);
	}
	
	public User(SimulationModel mainModel) throws CannotPlaceUserException {
		model = mainModel;
		id = numberOfIds++;
		// TODO: change to exponential distribution
		range = 20 + generator.nextInt(50);
		
		int retries = 5;
		boolean successfullyPlaced = false;
		while(retries-- >= 0) {
			Point newLocation = new Point(generator.nextInt(model.getWidth()),
									      generator.nextInt(model.getHeight()));
			if(model.validatePosition(newLocation, -1)) {
				successfullyPlaced = true;
				location = newLocation;
				break;
			}
		}
		if(!successfullyPlaced) throw new CannotPlaceUserException();
	}
}
