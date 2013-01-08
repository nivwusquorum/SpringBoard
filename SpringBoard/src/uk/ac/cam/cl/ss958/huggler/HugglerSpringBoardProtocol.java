package uk.ac.cam.cl.ss958.huggler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;

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
			// TODO: read request
			//Object message = reader.readObject();
			writer.writeObject(parent.getUserName());
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
		ObjectInputStream reader;
		try {
			reader = new ObjectInputStream(s.getInputStream());
			Object message = reader.readObject();
			if(message instanceof String) {
				Log.d(TAG, "==> Communicated with "+message +", wow! ");
			}
			s.close();		
		} catch(Exception e) {
			Log.w(TAG, "Cannot communicate with discovered peer (SpringBoard): " +e.getMessage());
		} finally {
			try {s.close();}catch(Exception e) {}
		}
	}

}
