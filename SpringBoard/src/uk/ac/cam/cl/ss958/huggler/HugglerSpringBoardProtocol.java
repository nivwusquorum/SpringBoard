package uk.ac.cam.cl.ss958.huggler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.security.PublicKey;
import java.util.List;

import android.util.Log;

public class HugglerSpringBoardProtocol implements HugglerProtocol {
	static String TAG = "Huggler";
	
	Huggler parent;
	
	public HugglerSpringBoardProtocol(Huggler parent) {
		this.parent = parent;
	}
	
	@Override
	public void answerClient(Socket s, String clientName) {
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
	public void askClient(Socket s, String clientName) {
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
		Object payload = parent.getDb().getMessageTable().getEncoded();
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
					PublicKey pk = parent.getDb().getKeyForFriend(ecm.getUser());
					if (pk == null) {
						Log.d("Huggler", "Received message, that I cannot decrypt");
						continue;
					}
					parent.getDb().getMessageTable().addEncodedMessage(ecm, pk);
				} else {
					Log.e("Huggler", "Not EncodedMessage!");
				}
			}
		} else {
			throw new Exception("Not that object");
		}
	}

}
