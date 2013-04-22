package uk.ac.cam.cl.ss958.huggler;

import java.net.Socket;

public interface HugglerProtocol {
	public void answerClient(Socket s);
	public void askClient(Socket s);
}
