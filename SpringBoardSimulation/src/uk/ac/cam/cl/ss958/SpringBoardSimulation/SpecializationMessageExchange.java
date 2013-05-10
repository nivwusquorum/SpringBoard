package uk.ac.cam.cl.ss958.SpringBoardSimulation;

public class SpecializationMessageExchange extends BloomFilterMessageExchange {
	
	private static final double SPEC = 2;
	
	@Override
	protected double getProbabilityOfDelivery(int msg, SpringBoardUser from,
			SpringBoardUser to) {
		double prev =  super.getProbabilityOfDelivery(msg, from, to);
		if (prev == ALWAYS_SEND || prev == DONT_SEND) 
			return prev;
		if (msg%SPEC == to.getID()%SPEC) return prev;
		else return 0.5*prev;
	}
}
