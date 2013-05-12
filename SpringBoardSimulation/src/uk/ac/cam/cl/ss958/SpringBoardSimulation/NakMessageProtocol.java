package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.org.apache.bcel.internal.generic.LLOAD;

public class NakMessageProtocol extends BloomFilterMessageExchange {
	private static final int REMS_CAPACITY = 1000;
	private static final boolean CHECK_THEORETICAL_UPPER_BOUND = false;

	private class CircularBuffer {
		Integer [] buffer;
		int next;
		public final int capacity;
		Map<Integer, Integer> s;
		public CircularBuffer(int capacity) {
			buffer = new Integer[capacity];
			next = 0;
			this.capacity=capacity;
			s = new HashMap<Integer, Integer>();
		}
		public void add(int x) {
			if (s.get(x) != null)
				return;
			if(buffer[next] != null) {
				s.remove(buffer[next]);
			}
			s.put(x, next);
			buffer[next++] = x;
			if (next == capacity) next = 0;
		}
		
		public Integer get(int w) {
			w=next-1-w;
			if (w < 0) w+=capacity;
			return buffer[w];
		}
		
		public boolean contains(Integer x) {
			return s.get(x) != null;
		}
		
		public void bumpUp(Integer x) {
			Integer xPos = s.get(x);
			assert xPos != null;
			int last = next-1;
			if (last < 0) last+=capacity;
			int temp = buffer[last];
			buffer[last] = buffer[xPos];
			buffer[xPos] = temp;
			s.put(buffer[last], last);
			s.put(buffer[xPos], xPos);
		}
	}
	
	private Map<Integer, CircularBuffer> REMs;
	private Map<Integer, Set<Integer>> messagesDeliveredTo;
	
	public NakMessageProtocol() {
		super();
		REMs = new HashMap<Integer, CircularBuffer>();
		messagesDeliveredTo = new HashMap<Integer, Set<Integer>>();
	}
	
	private void addRem(SpringBoardUser x, int mId) {
		CircularBuffer cb = REMs.get(x.getID());
		if (cb == null) {
			REMs.put(x.getID(), cb = new CircularBuffer(REMS_CAPACITY)); 
		}
		cb.add(mId);
	}
	
	@Override
	public void messageDelivered(Integer mId, SpringBoardUser to) {
		super.messageDelivered(mId, to);
		addRem(to, mId);
		Set<Integer> messagesTo = messagesDeliveredTo.get(to.getID());
		if (messagesTo == null) {
			messagesDeliveredTo.put(to.getID(), messagesTo = new HashSet<Integer>());
		}
		messagesTo.add(mId);
	}
	
	private boolean isMessageInREMs(Integer msg, SpringBoardUser accordingTo) {
		if (CHECK_THEORETICAL_UPPER_BOUND) {
			return SpringBoardUser.wasMessagesDelivered(msg);
		} else {
			CircularBuffer cb = REMs.get(accordingTo.getID());
			
			boolean ret = (cb != null && cb.contains(msg));
			if (ret) {
				cb.bumpUp(msg);
			}
			return ret;
		}
	}
	
	@Override
	protected double getProbabilityOfDelivery(int msg, SpringBoardUser from,
			SpringBoardUser to) {
		double prevResult = super.getProbabilityOfDelivery(msg, from, to);
		if (prevResult == ALWAYS_SEND || prevResult == DONT_SEND) 
			return prevResult;
		if (isMessageInREMs(msg, to)) {
			// have REM for it, ignore it and bump up REM.
			return DONT_SEND;
		} else {
			Set<Integer> myMessages = messagesDeliveredTo.get(to.getID());
			if (myMessages != null && myMessages.contains(msg)) {
				// if I don't have rem, but it was delivered to me, emit another rem.
				addRem(to,msg);
				return DONT_SEND;
			} else {
				// return what I got from bloom filter.
				return prevResult;
			}
		}
	}
	
	@Override
	protected void sendMessages(SpringBoardUser from,
			SpringBoardUser to,
			int maxMessages) {
		super.sendMessages(from, to, maxMessages);
		
		// exchange rems
		CircularBuffer cb = REMs.get(from.getID());
		if (cb == null) 
			return;
		// how about trying Math.min(maxMessages,100));
		for (int i=0; i<Math.min(cb.capacity/2, maxMessages); ++i) {
			Integer rem = cb.get(i);
			if (rem == null) break;
			if (to.messages.contains(rem) || r.nextInt(10) == 0) {
				addRem(to, rem);
			}
		}

	}
}
