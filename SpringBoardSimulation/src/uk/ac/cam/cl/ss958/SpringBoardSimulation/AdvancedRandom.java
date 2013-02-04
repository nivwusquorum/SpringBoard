package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.Random;


public class AdvancedRandom extends Random {
	double nextExponential(double lambda) {
		return -1/lambda*Math.log(nextDouble());
	}
	
	public AdvancedRandom(long seed) {
		super(seed);
	}
}
