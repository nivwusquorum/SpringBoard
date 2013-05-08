package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NakMessageProtocol extends BloomFilterMessageExchange{
	private static final int REMS_CAPACITY = 500;
	private static final boolean CHECK_THEORETICAL_UPPER_BOUND = true;

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
			while (w < 0) w+=capacity;
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

	@Override
	public boolean exchange(SpringBoardUser from,
			SpringBoardUser to,
			int maxMessages) {

		sendMessages(from, to, maxMessages);
		sendMessages(to,from, maxMessages);
		return true;

	}
	
	private Map<Integer, CircularBuffer> REMs;
	
	public NakMessageProtocol() {
		super();
		REMs = new HashMap<Integer, CircularBuffer>();
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
	}
	
	private boolean shouldRejectMessage(Integer msg, SpringBoardUser accordingTo) {
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
			if (shouldRejectMessage(msg, to) || shouldRejectMessage(msg, from)) {
				continue;
			}
			int invProbability =
					checkIfSeenAndSet(to, msg) ? 100 : 2;
			if (r.nextInt(invProbability) == 0) 
				to.messages.addMessage(msg, areFriends);
		}
		
		
		// exchange rems
		CircularBuffer cb = REMs.get(from.getID());
		if (cb == null) 
			return;
		for (int i=0; i<Math.min(cb.capacity, 2*maxMessages); ++i) {
			Integer rem = cb.get(i);
			if (rem == null) break;
			if (to.messages.contains(rem) || r.nextInt(100) == 0) {
				addRem(to, rem);
			}
		}

	}
}
