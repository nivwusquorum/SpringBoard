package uk.ac.cam.cl.ss958.huggler;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SqlKeyValueTable {
	private SQLiteDatabase db;
	private String name;
	
    private static final String KEY_ID = "id";
    private static final String KEY_PROPERTY = "property";
    private static final String KEY_VALUE = "value";
	
	SqlKeyValueTable(SQLiteDatabase db, String name) {
		this.name = name;
		this.db = db;
	}
	
	public void create() {
		String CREATE_DPROPERTIES_TABLE = "CREATE TABLE " + name + "("
                + KEY_ID + " INTEGER," + KEY_PROPERTY + " TEXT,"
                + KEY_VALUE + " TEXT," +
                "PRIMARY KEY (" + KEY_PROPERTY +")" + ")";
        db.execSQL(CREATE_DPROPERTIES_TABLE);
	}
	
	public boolean insert(String key, String value) {
		try {
	        ContentValues values = new ContentValues();
	        values.put(KEY_PROPERTY, key);
	        values.put(KEY_VALUE, value);
	        db.insertOrThrow(name, null, values);  
	        return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public String get(String key) {
		if (db == null) Log.wtf("Huggler", "That should not happen");
	    Cursor cursor = db.query(name, new String[] { KEY_ID,
	            KEY_PROPERTY, KEY_VALUE }, KEY_PROPERTY + "=?",
	            new String[] { key}, null, null, null, null);
	    if (cursor != null && cursor.moveToFirst()) {
	    	return cursor.getString(2);
	    } else {
	    	// Key nonexistent.
	    	return null;
	    }
	}
	
	public int update(String key, String value) {
    	ContentValues values = new ContentValues();
    	values.put(KEY_PROPERTY, key);
    	values.put(KEY_VALUE, value);

    	// updating row
    	return db.update(name, values, KEY_PROPERTY + " = ?",
    			new String[] { key});
	}
}
