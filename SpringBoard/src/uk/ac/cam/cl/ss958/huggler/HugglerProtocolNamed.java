package uk.ac.cam.cl.ss958.huggler;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.Property;

import android.util.Log;

public abstract class HugglerProtocolNamed implements HugglerProtocol {
	private static final String TAG = "Huggler";
	
	public String getMyHugglerId() {
		HugglerDatabase dbh = HugglerDatabase.get();
		return dbh.readProperty(Property.HUGGLER_ID);
	}
	
	private final HugglerIntroMessage receiveIntroMessage(Socket clientSocket) {
		try {
			ObjectInputStream reader = new ObjectInputStream(clientSocket.getInputStream());

			Object message = reader.readObject();
			HugglerIntroMessage introMessage;
			if(message instanceof HugglerIntroMessage) {
				introMessage = (HugglerIntroMessage)message;
			} else {
				throw new Exception("Intro message not received");
			}
			return introMessage;
		} catch(Exception e) {
			Log.d(TAG, "Cannot receive intro Message (" + e.getMessage()+")");
			return null;
		}
	}

	private final boolean sendIntroMessage(Socket clientSocket) {
		try {
			ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());

			HugglerIntroMessage introMessage =
					new HugglerIntroMessage(getName(), getMyHugglerId());
			writer.writeObject(introMessage);
			return true;
		} catch(Exception e) {
			Log.d(TAG, "Cannot send intro Message (" + e.getMessage()+")");
			return false;
		}
	}

	private final String determineAndValidateReceiver(Socket clientSocket, boolean initiating) {
		HugglerIntroMessage introMessage = null;
		boolean messageSent = false;
		if(initiating) {
			messageSent = sendIntroMessage(clientSocket);
			if(messageSent) {
				introMessage = receiveIntroMessage(clientSocket);
			}
		} else {
			introMessage = receiveIntroMessage(clientSocket);
			if (introMessage != null) {
				sendIntroMessage(clientSocket);
			}
		}
		if (!messageSent || introMessage == null) 
			return null;
		if (!introMessage.getProtocol().equals(getName()))
			return null;
		return introMessage.getName();
	}
	
	public final void answerClient(Socket s) {
		String otherHugglerId = determineAndValidateReceiver(s, false);
		answerNamedClient(s, otherHugglerId);
	}
		
	public final void askClient(Socket s) {
		String otherHugglerId = determineAndValidateReceiver(s, true);
		askNamedClient(s, otherHugglerId);

	}
	
	
	public abstract String getName();
	// It is protocol responsibility to close the socket
	public abstract void answerNamedClient(Socket s, String otherHugglerId);
	// It is protocol responsibility to close the socket
	public abstract void askNamedClient(Socket s, String otherHugglerId);
	
}
