package uk.ac.cam.cl.ss958.SpringBoardSimulation;

public interface MessageExchangeProtocol {
	public boolean exchange(SpringBoardUser from,
						 SpringBoardUser to,
						 int maxMessages);
	
	public void messageDelivered(Integer mId, SpringBoardUser to);

	public void messageCreated(Integer mId, SpringBoardUser to);
}
