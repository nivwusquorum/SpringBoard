package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import com.skjegstad.utils.BloomFilter;

public class LosyHashTableMessageExchange implements MessageExchangeProtocol {
	protected Random r = new Random(System.currentTimeMillis());
	private static final int TICKS_BETWEEN_WIPES = 200*RealisticModel.SIMULATION_DAY;
	private static final int HASH_TABLE_SLOTS = 30000;
	private static final double PROBABILITY_TRANSMIT_SEEN = 0.5;
	private static final double PROBABILITY_NOTSEEN = 1.0;
	
	protected static final double DONT_SEND = -1.0;
	protected static final double ALWAYS_SEND = 2.0;

	public LosyHashTableMessageExchange() {
		super();
		messagesSeenBy = new HashMap<Integer,  LosyHashTable>();
		nextWipe = new HashMap<Integer, Long>();
	}

	Map<Integer,  LosyHashTable> messagesSeenBy;
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
			 LosyHashTable bf = messagesSeenBy.get(x.getID());
			if (bf != null) bf.clear();
			nextWipe.put(x.getID(), RealisticModel.getStepsExecuted() +
					r.nextInt(2*TICKS_BETWEEN_WIPES));
		}
	}

	protected boolean checkIfSeenAndSet(User target, Integer msg) {
		 LosyHashTable bf = messagesSeenBy.get(target.getID());
		if (bf == null) {
			messagesSeenBy.put(target.getID(),
					bf = new LosyHashTable(HASH_TABLE_SLOTS));
		}
		if (bf.definitelyContain(msg)) {
			return true;
		} else {
			bf.add(msg);
			return false;
		}
	}
	
	protected double getProbabilityOfDelivery(int msg,
											   SpringBoardUser from,
										       SpringBoardUser to) {
		
		double result = 0.0;
		if (SpringBoardUser.mf != null) {
			SpringBoardUser target = SpringBoardUser.mf.getTarget(msg);
			if(target != null && target.getID() == to.getID()) {
				result = ALWAYS_SEND;
			} 
		}
			
		double filter = r.nextDouble();
		filter = Math.pow(filter, 2.0);
		
		double result2 =  checkIfSeenAndSet(to, msg) ? PROBABILITY_TRANSMIT_SEEN : PROBABILITY_NOTSEEN;
		if (result == ALWAYS_SEND) return result;
		else return filter*result2;
    }
	
	protected boolean trueWithProbability(double p) {
		return r.nextDouble()<p;
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
			double p = getProbabilityOfDelivery(msg, from, to);
			if (p == ALWAYS_SEND) p = 1.0;
			if (p == DONT_SEND) p = 0.0;
			//if (r.nextInt(1000)== 0)
			//	System.out.println("" + p);
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
