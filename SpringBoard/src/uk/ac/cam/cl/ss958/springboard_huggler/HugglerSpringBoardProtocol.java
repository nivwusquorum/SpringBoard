package uk.ac.cam.cl.ss958.springboard_huggler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.ss958.huggler.HugglerProtocol;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import uk.ac.cam.cl.ss958.springboard.content.ChatMessage;
import uk.ac.cam.cl.ss958.springboard.content.DatabaseContentProvider;
import uk.ac.cam.cl.ss958.springboard.content.EncodedChatMessage;
import uk.ac.cam.cl.ss958.springboard.content.SpringboardSqlSchema;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class HugglerSpringBoardProtocol extends  HugglerProtocol {
	static String TAG = "Huggler";
	
	ContentResolver cr;
	
	public HugglerSpringBoardProtocol(ContentResolver cr) {
		this.cr = cr;
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
	
    private static Uri messageTableUri = 
    		Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
    				"/" + SpringboardSqlSchema.Strings.Messages.NAME);
    
    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] {
        SpringboardSqlSchema.Strings.Messages.KEY_ID,
        SpringboardSqlSchema.Strings.Messages.KEY_MESSAGE,
    };
	
	private void sendMessages(Socket s) throws Exception {
		Cursor c = cr.query(messageTableUri, PROJECTION, null, null, null);
		c.moveToFirst();
		List<ChatMessage> messages = new ArrayList<ChatMessage>();
		while(!c.isAfterLast()) {
			messages.add(new ChatMessage("X", c.getString(1)));
			c.moveToNext();
		}
		ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());
		writer.writeObject(messages);
		/*Object payload = dbh.getMessageTable().getEncoded();
		ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());
		writer.writeObject(payload);*/
	}
	
	private void getMessages(Socket s) throws Exception {
		ObjectInputStream reader = new ObjectInputStream(s.getInputStream());
		Object message = reader.readObject();
		if (message instanceof List) {
			for (Object o : (List)message) {
				if (o instanceof ChatMessage) {
					String m = ((ChatMessage)o).getMessage();
	   				 ContentValues values = new ContentValues();
	                 values.put(SpringboardSqlSchema.Strings.Messages.KEY_MESSAGE, m);
	                 cr.insert(messageTableUri, values);
				}
			}
		}
		/*ObjectInputStream reader = new ObjectInputStream(s.getInputStream());
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
		}*/
	}

}
