package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class RandomModel extends SimulationModel{
	final Random generator = new Random (System.currentTimeMillis());

	private static final int SIMULATION_STEP_LENGH_MS = 5;
	public static final int SIMULATION_MOVING_INVLAMBDA = 1000;
	public static final int SIMULATION_MOVING_RESET_FREQ = 300;

	public static final int SIMULATION_WAITING_INVLAMBDA = 1000;	
	
	@Override
	public int getStepLengthMs() {
		return SIMULATION_STEP_LENGH_MS;
	}
	
	Map<Integer, RandomUser> users;

	public RandomModel(int width, int height) {
		super(width, height);
		users = new HashMap<Integer, RandomUser>();
	}



	public void simulationStep() {
		for (Integer id : users.keySet()) {
			users.get(id).maybeRandomlyMove();
		}
		onChange();
	}

	public boolean AddRandomUser() {
		try {
			RandomUser u = new RandomUser(this);
			users.put(u.getID(), u);
			onChange();
			return true;
		} catch(CannotPlaceUserException e) {
			onChange();
			return false;
		}
	}

	public void clearUsers() {
		users.clear();
		selectedUser = -1;
		onChange();
	}

	@Override
	public Map<Integer, ? extends User> getUsers() {
		return users;
	}
	
	private static class RandomUser extends User {
		RandomUser(SimulationModel m) throws CannotPlaceUserException {
			super(m);
			
			final SimulationModel model = m;
			
			moveGenerator = new NaturalMoveGenerator(possibleSteps.length, generator) {
				protected boolean validate(int move) {
					return model.validatePosition(getLocation().add(possibleSteps[move]), id);
				}
				
				protected int suggestedMove() {
					// How close is close.
					final int closeFactor = 10;
					int best = closeFactor*USER_RADIUS;
					int bestGuess = -1;
					if (getLocation().getX() < best) {
						best = getLocation().getX();
						bestGuess = 2;
					} 
					if(model.getWidth() - getLocation().getX() < best) {
						best = model.getWidth() - getLocation().getX();
						bestGuess = 0;
						
					}
					if (getLocation().getY() < best) {
						best = getLocation().getY();
						bestGuess = 3;
					}
					if (model.getHeight() - getLocation().getY() < best) {
						best = model.getHeight() - getLocation().getY();
						bestGuess = 1;
					}
					return bestGuess;
				}
				
			};
		}


		private static final Point [] possibleSteps = { new Point(-1,0),
			new Point(0,-1),
			new Point(1,0),
			new Point(0,1)
		};

		public void randomStep() throws CannotPlaceUserException {
			int step = moveGenerator.nextStep();
			if (step == -1) throw new CannotPlaceUserException();
			setLocation(getLocation().add(possibleSteps[step]));
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
						(int)generator.nextExponential(1.0/SIMULATION_MOVING_INVLAMBDA) :
							(int)generator.nextExponential(1.0/SIMULATION_WAITING_INVLAMBDA);
						if(isMoving) 
							moveGenerator.reset();
			}
			--ticksRemaining;
			if(ticksRemaining%SIMULATION_MOVING_RESET_FREQ == 0)
				moveGenerator.reset();
			if(isMoving) {
				try {
					randomStep();
				} catch(CannotPlaceUserException e) {
					// is Surrounded from each side then pitty, wait...
				}
			}
		}

	}
}
