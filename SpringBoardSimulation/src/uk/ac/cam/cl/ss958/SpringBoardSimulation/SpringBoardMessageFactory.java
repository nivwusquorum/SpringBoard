package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpringBoardMessageFactory {

	private class Message {
		public final SpringBoardUser from;
		public final SpringBoardUser to;
		public final double startingDistance;
		int noOfCopies;
		// if false was send to smb not on friend list.
		public final boolean toFriend;
		// if false means that message disappeared before it was delivered.
		boolean wasDelivered;
		
		public Message(SpringBoardUser from, SpringBoardUser to) {
			this.from = from;
			this.to = to;
			double dx = from.getLocation().getX() - to.getLocation().getX();
			double dy = from.getLocation().getY() - to.getLocation().getY();
			startingDistance = Math.sqrt(dx*dx + dy*dy);
			toFriend = from.getFriends().contains(to);
			wasDelivered = false;
			noOfCopies = 1;
		}
	}

	int noOfMessages;
	Map<Integer, Message> messages;
	List<Message> processedMessages;

	public SpringBoardMessageFactory() {
		messages = new HashMap<Integer, Message>();
		processedMessages = new ArrayList<Message>();
	}
	
	public Integer getMessage(SpringBoardUser from, SpringBoardUser to) {
		messages.put(noOfMessages, new Message(from, to));
		return noOfMessages++;
	}
	
	// code is still responsible to put this messages id in the to's list.
	public synchronized void deliverMessage(Integer mId, SpringBoardUser to) {
		assert mId < noOfMessages;
		Message m = messages.get(mId);
		if (m != null) {
			m.noOfCopies++;
			if(m.to.getID() == to.getID()) {
				m.wasDelivered = true;
				processedMessages.add(m);
				messages.remove(mId);
			}
		}
		// if m == null then messages was probably just delivered
	}
	
	public synchronized void deleteMessage(Integer mId) {
		assert mId < noOfMessages;
		Message m = messages.get(mId);
		if(m != null) {
			m.noOfCopies--;
			if (m.noOfCopies == 0) {				
				m.wasDelivered = false;
				processedMessages.add(m);
				messages.remove(mId);
				
			}
		}
		// if m == null then messages was probably just delivered
	}
}
