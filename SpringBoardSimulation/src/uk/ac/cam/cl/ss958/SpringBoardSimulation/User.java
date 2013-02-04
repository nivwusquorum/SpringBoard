package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Graphics;
import java.util.Random;
import java.util.ArrayList;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class User {
	public static final int SELECTION_BOLDNESS = 2;
	
	private final AdvancedRandom generator = 
			new AdvancedRandom (System.currentTimeMillis());
	static int numberOfIds = 0;
	
	private int range;
	private int id;
	private SimulationModel model;
	private Point location;

	
	
	public static final int USER_RADIUS = 7;
	
	public Point getLocation() {
		return location;
	}
	
	public int getID() {
		return id;
	}
	
	// For purpose of random movement simulation.
	// isMoving is true if currently performed action is moving.
	// isMoving is false if currently performed action is waiting.
	// ticksRemaining indicates number of ticks before switching to other type
	// of action.
	private boolean isMoving = false;
	private int ticksRemaining = 0;
	private NaturalMoveGenerator moveGenerator;
	
	public void maybeRandomlyMove() {
		/*if(id == 1) {
			System.out.printf("%s\n", isMoving ? "T" : "N");
		}*/
		if(ticksRemaining < 0) {
			isMoving = !isMoving;
			ticksRemaining = isMoving ? 
			    (int)generator.nextExponential(1.0/Constants.SIMULATION_MOVING_INVLAMBDA) :
			    (int)generator.nextExponential(1.0/Constants.SIMULATION_WAITING_INVLAMBDA);
			if(isMoving) 
				moveGenerator.reset();
		}
		--ticksRemaining;
		if(ticksRemaining%Constants.SIMULATION_MOVING_RESET_FREQ == 0)
			moveGenerator.reset();
		if(isMoving) {
			try {
				randomStep();
			} catch(CannotPlaceUserException e) {
				// is Surrounded from each side then pitty, wait...
			}
		}
	}
	
	private static Point [] possibleSteps = { new Point(-1,0),
											  new Point(0,-1),
											  new Point(1,0),
											  new Point(0,1)};
	
	public void randomStep() throws CannotPlaceUserException {
		int step = moveGenerator.nextStep();
		if (step == -1) throw new CannotPlaceUserException();
		setLocation(location.add(possibleSteps[step]));
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
		g.drawOval(location.getX()- range, location.getY()- range, 2*range, 2*range);
	}
	
	public User(SimulationModel mainModel) throws CannotPlaceUserException {
		model = mainModel;
		id = numberOfIds++;
		// TODO: change to exponential distribution
		range = 50 + generator.nextInt(100);
		
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
		
		moveGenerator = new NaturalMoveGenerator(possibleSteps.length, generator) {
			protected boolean validate(int move) {
				return model.validatePosition(location.add(possibleSteps[move]), id);
			}
			
			protected int suggestedMove() {
				// How close is close.
				final int closeFactor = 10;
				int best = closeFactor*USER_RADIUS;
				int bestGuess = -1;
				if (location.getX() < best) {
					best = location.getX();
					bestGuess = 2;
				} 
				if(model.getWidth() - location.getX() < best) {
					best = model.getWidth() - location.getX();
					bestGuess = 0;
					
				}
				if (location.getY() < best) {
					best = location.getY();
					bestGuess = 3;
				}
				if (model.getHeight() - location.getY() < best) {
					best = model.getHeight() - location.getY();
					bestGuess = 1;
				}
				return bestGuess;
			}
			
		};
	}
}
