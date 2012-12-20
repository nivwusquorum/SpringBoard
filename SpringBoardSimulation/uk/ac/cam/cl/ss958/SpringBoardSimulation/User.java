package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.Random;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class User {
	private static final Random generator = new Random (System.currentTimeMillis());

	private int range;

	public int getRange() {
		return range;
	}
	
	public void setRange(int r) {
		range = r;
	}
	
	public User() {
		// TODO: change to exponential distribution
		range = 20 + generator.nextInt(50);
	}
}
