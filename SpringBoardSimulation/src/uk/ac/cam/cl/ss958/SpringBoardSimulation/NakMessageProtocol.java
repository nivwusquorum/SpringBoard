package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NakMessageProtocol extends BloomFilterMessageExchange{
	private static final int REMS_CAPACITY = 500;

	private class CircularBuffer {
		Integer [] buffer;
		int last;
		public final int capacity;
		Set<Integer> s;
		public CircularBuffer(int capacity) {
			buffer = new Integer[capacity];
			last = 0;
			this.capacity=capacity;
			s = new HashSet<Integer>();
		}
		public void add(int x) {
			if (s.contains(x))
				return;
			if(buffer[last] != null) {
				s.remove(buffer[last]);
			}
			buffer[last++] = x;
			s.add(x);
			if (last == capacity) last = 0;
		}
		
		public Integer get(int w) {
			w=-w;
			while (w < 0) w+=capacity;
			return buffer[w];
		}
		
		public boolean contains(Integer x) {
			return s.contains(x);
		}
	}

	private Map<Integer, CircularBuffer> REMs;
	
	public NakMessageProtocol() {
		super();
		REMs = new HashMap<Integer, CircularBuffer>();
	}
	
	private void addRem(SpringBoardUser x, int mId) {
		CircularBuffer cb = REMs.get(x.getID());
		if (cb == null) {
			REMs.put(x.getID(), new CircularBuffer(REMS_CAPACITY)); 
		}
		cb.add(mId);
	}
	
	@Override
	public void messageDelivered(Integer mId, SpringBoardUser to) {
		super.messageDelivered(mId, to);
		addRem(to, mId);
	}
	
	private boolean shouldRejectMessage(Integer msg, SpringBoardUser accordingTo) {
		CircularBuffer cb = REMs.get(accordingTo.getID());
		return cb != null && cb.contains(msg);
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
			if (shouldRejectMessage(msg, to)) {
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
			if (to.messages.contains(rem) || r.nextInt(200) == 0) {
				addRem(to, rem);
			}
		}

	}
}
