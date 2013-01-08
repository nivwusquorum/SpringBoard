package uk.ac.cam.cl.ss958.huggler;

import java.net.Socket;

public interface HugglerProtocol {
	public String getName();
	// It is protocol responsibility to close the socket
	public void answerClient(Socket s, String clientName);
	// It is protocol responsibility to close the socket
	public void askClient(Socket s, String clientName);
	
}
