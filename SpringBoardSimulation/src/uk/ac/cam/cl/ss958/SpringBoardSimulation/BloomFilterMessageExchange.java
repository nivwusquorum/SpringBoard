package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import com.skjegstad.utils.BloomFilter;

public class BloomFilterMessageExchange implements MessageExchangeProtocol {
	protected Random r = new Random(System.currentTimeMillis());
	private static final int TICKS_BETWEEN_WIPES = RealisticModel.SIMULATION_DAY;

	public BloomFilterMessageExchange() {
		messagesSeenBy = new HashMap<Integer, BloomFilter<Integer>>();
		nextWipe = new HashMap<Integer, Long>();
	}

	Map<Integer, BloomFilter<Integer>> messagesSeenBy;
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
			BloomFilter<Integer> bf = messagesSeenBy.get(x.getID());
			if (bf != null) bf.clear();
			nextWipe.put(x.getID(), RealisticModel.getStepsExecuted() +
					r.nextInt(2*TICKS_BETWEEN_WIPES));
		}
	}

	protected boolean checkIfSeenAndSet(User target, Integer msg) {
		if (messagesSeenBy.get(target.getID()) == null) {
			double c = 0.01;
			int totalMessages = 1000;
			messagesSeenBy.put(target.getID(),
					new BloomFilter<Integer>(c, totalMessages,
							(int)(c*Math.log(2))));
		}
		BloomFilter<Integer> bf = messagesSeenBy.get(target.getID());
		if (bf.contains(msg)) {
			return true;
		} else {
			bf.add(msg);
			return false;
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
			int invProbability =
					checkIfSeenAndSet(to, msg) ? 100 : 2;
			if (r.nextInt(invProbability) == 0) 
				to.messages.addMessage(msg, areFriends);
		}
	}




	@Override
	public void messageDelivered(Integer mId, SpringBoardUser to) {

	}

}
