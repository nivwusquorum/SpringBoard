package uk.ac.cam.cl.ss958.huggler;

import java.util.Random;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class HugglerDatabase extends SQLiteOpenHelper {
	 
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "Huggler";
 
    // Debug Properties table
    private static final String TABLE_DEBUG = "debug_properties";
    private static SqlKeyValueTable debug_table;
    
    public enum DebugProperty {
    	DEVICE_BOOTS ("device_boots"),
    	LOOKS_NUMBER ("looks");
    	
    	private String name;
    	
    	public String getName() {
    		return name;
    	}
    	
    	DebugProperty(String name) {
    		this.name = name;
    	}
    }
    
    // Debug Properties table
    private static final String TABLE_PROPERTIES = "properties";
    private static SqlKeyValueTable properties_table;
    
    public enum Property {
    	HUGGLER_ID ("huggler_id");
    	
    	private String name;
    	
    	public String getName() {
    		return name;
    	}
    	
    	Property(String name) {
    		this.name = name;
    	}
    }
    
    private static final String TABLE_MESSAGES = "chatmessages";
    private static SQLChatMessagesTable messages_table;
    
    private SQLiteDatabase db;
    
    public HugglerDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }
    
    public SQLiteDatabase getDb() {
    	return db;
    }
 
    private boolean tables_initialized = false;
    
    public void initTables(SQLiteDatabase db) {
    	if (!tables_initialized) {
    		tables_initialized = true;
	        debug_table = new SqlKeyValueTable(db, TABLE_DEBUG);
	        properties_table = new SqlKeyValueTable(db, TABLE_PROPERTIES);
	        messages_table = new SQLChatMessagesTable(db, TABLE_MESSAGES);
    	}
    }
    
    public SQLChatMessagesTable getMessageTable() {
    	return messages_table;
    }
    
    @Override
    public void onOpen(SQLiteDatabase db) {
    	initTables(db);
    }
    
    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
    	initTables(db);
    	// Create Table with debug properties
        debug_table.create();
        
        // Initiate variable holding number of boots.
        debug_table.insert(DebugProperty.DEVICE_BOOTS.getName(), "0");
        
        // Initiate variable holding number of triggered looks.
        debug_table.insert(DebugProperty.LOOKS_NUMBER.getName(), "0");
        
    	// Create Table with normal properties
        properties_table.create();        
        
        Random generator = new Random();
        String randomId = "huggler_user" + generator.nextInt(1000);
        properties_table.insert(Property.HUGGLER_ID.getName(), randomId);
        
        messages_table.create();
    }
    
    public String readProperty(Property p) {
    	return properties_table.get(p.getName());
    }
    
    public String readDebugProperty(DebugProperty d) {
    	return debug_table.get(d.getName());
    }
    
    
    public void incrementDebugProperty(DebugProperty d) {
    	int new_value = Integer.parseInt(debug_table.get(d.getName()))+1;
    	debug_table.update(d.getName(), String.valueOf(new_value));
    }
    
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	// add subsequent modificiations
    	// if oldVersion < 1 && newVersion >=1 create table debug_properties ...  
    }
}