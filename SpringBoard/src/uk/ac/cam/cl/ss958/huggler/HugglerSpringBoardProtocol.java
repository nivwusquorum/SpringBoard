package uk.ac.cam.cl.ss958.huggler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
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
			ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());
			writer.writeObject(parent.getDb().getMessageTable().get(null));
			ObjectInputStream reader = new ObjectInputStream(s.getInputStream());
			Object message = reader.readObject();
			if(message instanceof List) {
				for(Object o: (List)message) {
					if(o instanceof ChatMessage) {
						parent.getDb().getMessageTable().addMessage((ChatMessage)o);
					}
				}
			} else {
				throw new Exception("Not that object");
			}
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
			ObjectInputStream reader = new ObjectInputStream(s.getInputStream());
			Object message = reader.readObject();
			if(message instanceof List) {
				for(Object o: (List)message) {
					if(o instanceof ChatMessage) {
						parent.getDb().getMessageTable().addMessage((ChatMessage)o);
					}
				}
			} else {
				throw new Exception("Not that object");
			}
			ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());
			writer.writeObject(parent.getDb().getMessageTable().get(null));
			s.close();		
		} catch(Exception e) {
			Log.w(TAG, "Cannot communicate with discovered peer (SpringBoard): " +e.getMessage());
		} finally {
			try {s.close();}catch(Exception e) {}
		}
	}

}
