package uk.ac.cam.cl.ss958.huggler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HugglerDatabase extends SQLiteOpenHelper {
	 
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "Huggler";
 
    // Debug Properties table
    private static final String TABLE_DEBUG = "debug";
    private static final String KEY_ID = "id";
    private static final String KEY_PROPERTY = "property";
    private static final String KEY_VALUE = "value";
    public static final String DPROPERTY_DEVICE_BOOTS = "device_boots";
    public static final String DPROPERTY_LOOKS_NUMBER = "looks";

    
    public HugglerDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
    	// Create Table with debug properties
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_DEBUG + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_PROPERTY + " TEXT,"
                + KEY_VALUE + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
        
        // Initiate variable holding number of boots.
        ContentValues values = new ContentValues();
        values.put(KEY_PROPERTY, DPROPERTY_DEVICE_BOOTS); // Contact Name
        values.put(KEY_VALUE, "0"); // Contact Phone Number
        db.insert(TABLE_DEBUG, null, values);  
        
        // Initiate variable holding number of triggered looks.
        values = new ContentValues();
        values.put(KEY_PROPERTY, DPROPERTY_LOOKS_NUMBER); // Contact Name
        values.put(KEY_VALUE, "0"); // Contact Phone Number
        db.insert(TABLE_DEBUG, null, values);  
    }
    
    public int readDebugProperty(String debug_property) {
    	// Read out current number of boots.
    	SQLiteDatabase db = this.getWritableDatabase();
        
        Cursor cursor = db.query(TABLE_DEBUG, new String[] { KEY_ID,
                KEY_PROPERTY, KEY_VALUE }, KEY_PROPERTY + "=?",
                new String[] { debug_property}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
     
        return Integer.parseInt(cursor.getString(2));
    }
    
    private int incrementNumericalProperty(String property) {
    	// Read out current number of boots.
    	SQLiteDatabase db = this.getWritableDatabase();
    	
    	ContentValues values = new ContentValues();
    	values.put(KEY_PROPERTY, property);
    	values.put(KEY_VALUE, String.valueOf(readDebugProperty(property) + 1));

    	// updating row
    	return db.update(TABLE_DEBUG, values, KEY_PROPERTY + " = ?",
    			new String[] { property});
    }
    
    public void onBoot() {
    	incrementNumericalProperty(DPROPERTY_DEVICE_BOOTS);
    }
    
    public void onLookAround() {
    	incrementNumericalProperty(DPROPERTY_LOOKS_NUMBER);
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	// add subsequent modificiations
    	// if oldVersion < 1 && newVersion >=1 create table debug_properties ...  
    }
}