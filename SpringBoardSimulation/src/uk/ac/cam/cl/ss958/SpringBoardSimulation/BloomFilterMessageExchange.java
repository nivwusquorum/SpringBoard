package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import com.skjegstad.utils.BloomFilter;

public class BloomFilterMessageExchange implements MessageExchangeProtocol {
	protected Random r = new Random(System.currentTimeMillis());
	private static final int TICKS_BETWEEN_WIPES = RealisticModel.SIMULATION_DAY;
	private static final int BLOOM_FILTER_N = 1000000;
	private static final double BLOOM_FILTER_C = 0.05;
	private static final double PROBABILITY_TRANSMIT_BLOOM = 0.5;
	private static final double PROBABILITY_NOBLOOM = 1.0;
	private static final int BLOOM_HASHES = 34;

	public BloomFilterMessageExchange() {
		super();
		messagesSeenBy = new HashMap<Integer, FastBloomFilter>();
		nextWipe = new HashMap<Integer, Long>();
	}

	Map<Integer, FastBloomFilter> messagesSeenBy;
	Map<Integer, Long> nextWipe;
	@Override
	public boolean exchange(SpringBoardUser from,
			SpringBoardUser to,
			int maxMessages) {

		sendMessages(from, to, maxMessages);
		sendMessages(to,from, maxMessages);
		return true;

	}

	protected void checkWipe(User x) {
		Long nextWipeX = nextWipe.get(x.getID());
		if (nextWipeX == null) {
			nextWipe.put(x.getID(), RealisticModel.getStepsExecuted() +
					r.nextInt(2*TICKS_BETWEEN_WIPES));
			return;
		}
		if (RealisticModel.getStepsExecuted() >=nextWipeX) {
			FastBloomFilter bf = messagesSeenBy.get(x.getID());
			if (bf != null) bf.clear();
			nextWipe.put(x.getID(), RealisticModel.getStepsExecuted() +
					r.nextInt(2*TICKS_BETWEEN_WIPES));
		}
	}

	protected boolean checkIfSeenAndSet(User target, Integer msg) {
		FastBloomFilter bf = messagesSeenBy.get(target.getID());
		if (bf == null) {
			messagesSeenBy.put(target.getID(),
					bf = new FastBloomFilter((int)(BLOOM_FILTER_C*BLOOM_FILTER_N),
							BLOOM_HASHES));
		}
		if (bf.mightContain(msg)) {
			return true;
		} else {
			bf.add(msg);
			return false;
		}
	}
	
	protected double getProbabilityOfDelivery(int msg,
											   SpringBoardUser from,
										       SpringBoardUser to) {
		return checkIfSeenAndSet(to, msg) ? PROBABILITY_TRANSMIT_BLOOM : PROBABILITY_NOBLOOM;
	}
	
	protected boolean trueWithProbability(double p) {
		return r.nextDouble()<p;
	}
	
	protected double getFinalDeliveryPriority(int msg,
										   SpringBoardUser from,
										   SpringBoardUser to) {

		double probability = getProbabilityOfDelivery(msg, from, to);
		boolean DEBUG = false;
		if(DEBUG && r.nextInt(10000) == 0) 
			System.out.println("probability of delivery" + probability);
		// Check if message target is TO. If it is always accept.
		// This needs to be able to be done after computing hypothetical result,
		// as the algorithms underneath rely on getProbabilityOfDelivery being
		// called regardless of the target.
		if (SpringBoardUser.mf != null) {
			SpringBoardUser target = SpringBoardUser.mf.getTarget(msg);
			if(target != null && target.getID() == to.getID()) {
				return 1.0;
			} else {
				return probability;
			}
		} else  {
			return probability;
		}
	}

	protected void sendMessages(SpringBoardUser from,
			SpringBoardUser to,
			int maxMessages) {
		checkWipe(from);
		boolean areFriends = to.getFriends().contains(from);
		int messagesSent = 0;
		for (int i=0; i<from.messages.getSize() && messagesSent < maxMessages; ++i) {
			++messagesSent;
			Integer msg = (Integer)from.messages.getElementAt(i);
			assert msg != null;
			double p = getFinalDeliveryPriority(msg, from, to);
			if (trueWithProbability(p)) 
				to.messages.addMessage(msg, areFriends, p);
		}
	}




	@Override
	public void messageDelivered(Integer mId, SpringBoardUser to) {

	}

	@Override
	public void messageCreated(Integer mId, SpringBoardUser to) {
	}

	@Override
	public void step() {}
}
