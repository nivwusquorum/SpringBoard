package uk.ac.cam.cl.ss958.springboard_huggler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.security.PublicKey;
import java.util.List;

import uk.ac.cam.cl.ss958.huggler.EncodedChatMessage;
import uk.ac.cam.cl.ss958.huggler.HugglerProtocol;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;

import android.util.Log;

public class HugglerSpringBoardProtocol extends  HugglerProtocol {
	static String TAG = "Huggler";
	
	HugglerDatabase dbh;
	
	public HugglerSpringBoardProtocol() {
		dbh = HugglerDatabase.get();
	}
	
	@Override
	public void answerNamedClient(Socket s, String clientName) {
		try {
			Log.d("Huggler", "answeringClient");
			sendMessages(s);
			getMessages(s);
		} catch(Exception e) {
			Log.w(TAG, "Unsuccessful exchange (Springboard side) : " + e.getMessage());
		} finally {
			try {s.close();}catch(Exception e) {}
		}
	}

	@Override
	public String getName() {
		return "SpringBoard";
	}

	@Override
	public void askNamedClient(Socket s, String clientName) {
		try {
			Log.d("Huggler", "askingClient");
			getMessages(s);
			sendMessages(s);
		} catch(Exception e) {
			Log.w(TAG, "Cannot communicate with discovered peer (SpringBoard): " +e.getMessage());
		} finally {
			try {s.close();}catch(Exception e) {}
		}
	}
	
	private void sendMessages(Socket s) throws Exception {
		Object payload = dbh.getMessageTable().getEncoded();
		ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());
		writer.writeObject(payload);
	}
	
	private void getMessages(Socket s) throws Exception {
		ObjectInputStream reader = new ObjectInputStream(s.getInputStream());
		Object message = reader.readObject();
		if(message instanceof List) {
			for(Object o: (List)message) {
				if(o instanceof EncodedChatMessage) {
					EncodedChatMessage ecm = (EncodedChatMessage)o;
					PublicKey pk = dbh.getFriendsTable().getKeyForFriend(ecm.getUser());
					if (pk == null) {
						Log.d(TAG, "Received message, that I cannot decrypt");
						continue;
					}
					dbh.getMessageTable().addEncodedMessage(ecm, pk);
				} else {
					Log.e(TAG, "Not EncodedMessage!");
				}
			}
		} else {
			throw new Exception("Not that object");
		}
	}

}
