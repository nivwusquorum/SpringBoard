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
	private static final double PROBABILITY_TRANSMIT_SEEN = 0.1;
	private static final double PROBABILITY_NOTSEEN = 0.5;
	private static final int BLOOM_HASHES = 34;
	
	protected static final double DONT_SEND = -1.0;
	protected static final double ALWAYS_SEND = 2.0;

	public BloomFilterMessageExchange() {
		super();
		messagesSeenBy = new HashMap<Integer,  FastBloomFilter>();
		nextWipe = new HashMap<Integer, Long>();
	}

	Map<Integer,  FastBloomFilter> messagesSeenBy;
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
		
		if (nextWipeX == null || RealisticModel.getStepsExecuted() >=nextWipeX) {
			FastBloomFilter bf = getBf(x.getID());
			bf.clear();
			nextWipe.put(x.getID(), RealisticModel.getStepsExecuted() +
					r.nextInt(2*TICKS_BETWEEN_WIPES));
		}
	}
	
	private FastBloomFilter getBf(int id) {
		FastBloomFilter bf = messagesSeenBy.get(id);
		if (bf == null) {
			messagesSeenBy.put(id,
					bf = new FastBloomFilter((int)(BLOOM_FILTER_C*BLOOM_FILTER_N),
							BLOOM_HASHES));
		}
		return bf;
	}

	protected boolean checkIfSeen(User target, Integer msg) {
		FastBloomFilter bf = getBf(target.getID());
		return bf.mightContain(msg);
	}
	
	protected void setSeen(User target, Integer msg) {
		FastBloomFilter bf = getBf(target.getID());
		bf.add(msg);
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
			
		double result2 =  checkIfSeen(to, msg) ? PROBABILITY_TRANSMIT_SEEN : PROBABILITY_NOTSEEN;
		if (result == ALWAYS_SEND) return result;
		else return result2;
    }
	
	protected boolean trueWithProbability(double p) {
		return r.nextDouble()<p;
	}
	
	protected void addMessage(SpringBoardUser to, int msgId, boolean fromFriend, double priority) {
		to.messages.addMessage(msgId, fromFriend, priority);
		setSeen(to, msgId);
	}

	protected void sendMessages(SpringBoardUser from,
			SpringBoardUser to,
			int maxMessages) {
		boolean areFriends = to.getFriends().contains(from);
		int messagesSent = 0;
		for (int i=0; i<from.messages.getSize() && messagesSent < maxMessages; ++i) {
			++messagesSent;
			Integer msg = (Integer)from.messages.getElementAt(i);
			assert msg != null;
			double p = getProbabilityOfDelivery(msg, from, to);
			if (p == ALWAYS_SEND) p = 1.0;
			if (p == DONT_SEND) p = 0.0;
			if (trueWithProbability(p)) {
				addMessage(to, msg, areFriends, p);
				to.messages.addMessage(msg, areFriends, p);
			}
		}
	}




	@Override
	public void messageDelivered(Integer mId, SpringBoardUser to) {

	}

	@Override
	public void messageCreated(Integer mId, SpringBoardUser to) {
	}

	@Override
	public void step() {
		Map<Integer, ? extends User> users = null;
		try {
			if (RealisticModel.getStepsExecuted() == 0) {
				return;
			}
			users = RealisticModel.getLatestInstance().getUsers();
		} catch (NullPointerException e) {
			return;
		}
		for (Integer i : users.keySet()) {
			checkWipe(users.get(i));
		}
		
	}
}
