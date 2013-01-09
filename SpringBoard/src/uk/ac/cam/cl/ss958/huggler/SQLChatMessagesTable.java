package uk.ac.cam.cl.ss958.huggler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SQLChatMessagesTable {
	private static final String TAG = "Huggler";
	private SQLiteDatabase db;
	private String name;

	private static final String KEY_ID = "id";
	private static final String KEY_WHEN = "date";
	private static final String KEY_USER = "user";
	private static final String KEY_MESSAGE = "message";
	private static final String[] allColumns = 
		{ KEY_ID, KEY_WHEN, KEY_USER, KEY_MESSAGE };

	SQLChatMessagesTable(SQLiteDatabase db, String name) {
		this.name = name;
		this.db = db;
	}

	public void create() {
		String CREATE_DPROPERTIES_TABLE = 
				"CREATE TABLE " + name + "("+ 
			    KEY_ID + " INTEGER," + 
				KEY_WHEN + " INTEGER," +
				KEY_USER + " TEXT," + 
				KEY_MESSAGE + " TEXT, " +
				"PRIMARY KEY (" + KEY_USER + "," + KEY_WHEN + ")"+ ")";
		db.execSQL(CREATE_DPROPERTIES_TABLE);
	}

	public void addMessage(ChatMessage m) {
		ContentValues values = new ContentValues();
		values.put(KEY_USER, m.getUser());
		values.put(KEY_MESSAGE, m.getMessage());
		values.put(KEY_WHEN, m.getTimestamp().getTime());
		try {
			db.insertOrThrow(name, null, values);
		} catch(android.database.sqlite.SQLiteConstraintException e) {
			// Log.d(TAG, "Duplicate message (hopefully).");
		}

	}
	

	public List<ChatMessage> get(Integer limit) {
		List<ChatMessage> result = new ArrayList<ChatMessage>();
		
		String maybeLimit = (limit == null) ? null : String.valueOf(limit); 
		Cursor cursor = db.query(name,
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

}
