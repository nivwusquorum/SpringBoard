package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import uk.ac.cam.cl.ss958.SpringBoardSimulation.SpringBoardUser.MessageClass;

import com.skjegstad.utils.BloomFilter;

public class BloomFilterMessageExchange implements MessageExchangeProtocol {
	protected Random r = new Random(System.currentTimeMillis());
	private static final int TICKS_BETWEEN_WIPES = RealisticModel.SIMULATION_DAY;
	private static final int BLOOM_FILTER_N = 1000000;
	private static final double BLOOM_FILTER_C = 0.05;
	private static final double PROBABILITY_TRANSMIT_SEEN = 0.1;
	private static final double PROBABILITY_NOTSEEN = 0.5;
	private static final int BLOOM_HASHES = 34;

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
		
			
		return checkIfSeen(to, msg) ? PROBABILITY_TRANSMIT_SEEN : PROBABILITY_NOTSEEN;

    }
	
	protected boolean trueWithProbability(double p) {
		return r.nextDouble()<p;
	}
	
	
	protected static class AbsoluteRule {
		public static enum RuleType {
			NONE,
			NEVERDELIVER,
			ALWAYSDELIVER;
		}
		
		public final RuleType type;
		public final int priority;
		
		private static Set<Integer> prioritiesUsed = new HashSet<Integer>();
		
		private AbsoluteRule(RuleType type, int priority) {
			this.type = type;
			if (priority != 0) {
				assert !prioritiesUsed.contains(priority);
				prioritiesUsed.add(priority);
			}
			this.priority = priority;
		}
		
		public static AbsoluteRule none() {
			return new AbsoluteRule(RuleType.NONE, 0);// TODO Auto-generated constructor stub
		}
		
		// currently also results in putting message in friends buffer.
		public static AbsoluteRule always(int p) {
			assert p >= 1;
			return new AbsoluteRule(RuleType.ALWAYSDELIVER, p);// TODO Auto-generated constructor stub
		}
		
		public static AbsoluteRule never(int p) {
			assert p >= 1;
			return new AbsoluteRule(RuleType.NEVERDELIVER, p);// TODO Auto-generated constructor stub
		}
	}
	
	private static final AbsoluteRule alwaysSendBecauseFriendsOptimization =
			AbsoluteRule.always(1);
	
	protected List<AbsoluteRule> getAbsoluteRules(int msg, SpringBoardUser to) {
		List<AbsoluteRule> rules = new ArrayList<AbsoluteRule>();
		
		SpringBoardUser sender = SpringBoardUser.mf.getSender(msg);
		SpringBoardUser target = SpringBoardUser.mf.getTarget(msg);
		
		if (sender == null || target == null)
			return rules;
		
		if (to.getFriends().contains(target) || to.getFriends().contains(sender) || target.getID() == to.getID()) {
			rules.add(alwaysSendBecauseFriendsOptimization);
		}
		
		return rules;
	}
	
	
	
	private AbsoluteRule getAbsoluteRule(int msg, SpringBoardUser to) {
		List<AbsoluteRule> rules = getAbsoluteRules(msg, to);
		AbsoluteRule best = AbsoluteRule.none();
		for (AbsoluteRule ar : rules) {
			if (best.priority < ar.priority) {
				best = ar;
			}
		}
		return best;
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
			
			
			switch(getAbsoluteRule(msg,to).type) {
				case ALWAYSDELIVER:
					to.messages.addMessage(msg, MessageClass.REGARDS_ME_OR_FRIENDS, 1.0);
					continue;
				case NEVERDELIVER:
					continue;
				case NONE:
					break;
			}
			
			double p = getProbabilityOfDelivery(msg, from, to);

			if (trueWithProbability(p)) {
				setSeen(to, (int)msg);
				to.messages.addMessage((int)msg, 
									   areFriends ? MessageClass.FORWARDED_BY_FRIENDS : MessageClass.FORWARDED_BY_OTHERS,
									   p);
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
