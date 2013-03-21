package uk.ac.cam.cl.ss958.springboard.content;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.ac.cam.cl.ss958.toolkits.SerializableToolkit;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SqlChatMessagesTable {
	private static final String TAG = "Huggler";
	private SQLiteDatabase db;
	private String name;

	public static final String KEY_ID = "id";
	public static final String KEY_WHEN = "date";
	public static final String KEY_USER = "user";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_ENCODED_MESSAGE = "encoded_message";

	private static final String[] allColumns = 
		{ KEY_ID, KEY_WHEN, KEY_USER, KEY_MESSAGE };

	public SqlChatMessagesTable(SQLiteDatabase db, String name) {
		this.name = name;
		this.db = db;
	}

	public void create() {
		String CREATE_CHAT_TABLE = 
				"CREATE TABLE " + name + "_normal" + "("+ 
			    KEY_ID + " INTEGER," + 
				KEY_WHEN + " INTEGER," +
				KEY_USER + " TEXT," + 
				KEY_MESSAGE + " TEXT, " +
				"PRIMARY KEY (" + KEY_USER + "," + KEY_WHEN + ")"+ ")";
		db.execSQL(CREATE_CHAT_TABLE);
		
		String CREATE_ECHAT_TABLE = 
				"CREATE TABLE " + name + "_encoded" + "("+ 
			    KEY_ID + " INTEGER PRIMARY KEY," +
				KEY_ENCODED_MESSAGE + " TEXT" + ")";
		db.execSQL(CREATE_ECHAT_TABLE);
	}

	public void addEncodedMessage(EncodedChatMessage ecm, PublicKey key) {
		try {
			ContentValues values = new ContentValues();
			String serializedEcm = SerializableToolkit.toString(ecm);
			values.put(KEY_ENCODED_MESSAGE, serializedEcm);	
			db.insertOrThrow(name + "_encoded", null, values);
			addMessage(ecm.getChatMessage(key));
		} catch(Exception e) {
			Log.d("Huggler", "Problem decoding message (" + e.getMessage() + ")");
			for(StackTraceElement el : e.getStackTrace()) {
				Log.d("Huggler", el.toString());
			}
		}
	}
	
	
	private void addMessage(ChatMessage m) {
		ContentValues values = new ContentValues();
		values.put(KEY_USER, m.getUser());
		values.put(KEY_MESSAGE, m.getMessage());
		values.put(KEY_WHEN, m.getTimestamp().getTime());
		try {
			db.insertOrThrow(name +"_normal", null, values);
		} catch(android.database.sqlite.SQLiteConstraintException e) {
			// Log.d(TAG, "Duplicate message (hopefully).");
		}
	}
	
	public Cursor getCursor(Integer limit) {
		String maybeLimit = (limit == null) ? null : String.valueOf(limit); 
		Cursor cursor = db.query(name + "_normal",
								 allColumns, null, null, null, 
								 null, KEY_WHEN + " DESC", maybeLimit );	

		return cursor;

	}

	public List<ChatMessage> get(Integer limit) {
		List<ChatMessage> result = new ArrayList<ChatMessage>();
		
		String maybeLimit = (limit == null) ? null : String.valueOf(limit); 
		Cursor cursor = db.query(name + "_normal",
								 allColumns, null, null, null, 
								 null, KEY_WHEN + " DESC", maybeLimit );
	
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			ChatMessage m = new ChatMessage(
					cursor.getString(2),
					cursor.getString(3),
					new Date(cursor.getLong(1)));
			result.add(m);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return result;
	}
	
	public List<EncodedChatMessage> getEncoded() {
		try {
			Cursor cursor = db.query(name + "_encoded",
					 new String[] {KEY_ID, KEY_ENCODED_MESSAGE }, null, null, null, 
					 null, null, null );
			cursor.moveToFirst();
			List<EncodedChatMessage> result = new ArrayList<EncodedChatMessage>();
			while (!cursor.isAfterLast()) {
				EncodedChatMessage ecm = 
					(EncodedChatMessage) SerializableToolkit.fromString(cursor.getString(1));
				result.add(ecm);
				cursor.moveToNext();
			}
			cursor.close();
			return result;
		} catch(Exception e) {
			return null;
		}
	}

}
