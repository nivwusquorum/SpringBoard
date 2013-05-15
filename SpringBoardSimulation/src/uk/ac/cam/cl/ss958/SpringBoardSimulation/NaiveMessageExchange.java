package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.Random;

import uk.ac.cam.cl.ss958.SpringBoardSimulation.SpringBoardUser.MessageClass;

public class NaiveMessageExchange implements MessageExchangeProtocol {
	private Random r = new Random(System.currentTimeMillis());
	
	// Always sends maxMessages freshest messages per user and does not continue sending. 
	@Override
	public boolean exchange(SpringBoardUser from,
						 SpringBoardUser to,
						 int maxMessages) {
		sendMessages(from, to, maxMessages);
		sendMessages(to,from, maxMessages);
		return true;
		
	}
	

	
	private void sendMessages(SpringBoardUser from,
							  SpringBoardUser to,
							  int maxMessages) {
		boolean areFriends = to.getFriends().contains(from);
		int messagesSent = 0;
		for (int i=0; i<from.messages.getSize() && messagesSent < maxMessages; ++i) {
			++messagesSent;
			if (r.nextInt(10) == 0 || maxMessages < 100) 
				to.messages.addMessage((Integer)from.messages.getElementAt(i),
										areFriends ? MessageClass.FORWARDED_BY_FRIENDS :
										MessageClass.FORWARDED_BY_OTHERS, 0.5);
		}
	}



	@Override
	public void messageDelivered(Integer mId, SpringBoardUser to) {
	}



	@Override
	public void messageCreated(Integer mId, SpringBoardUser to) {
		// TODO Auto-generated method stub
	}

	@Override
	public void step() {}
}
