package uk.ac.cam.cl.ss958.huggler.databases;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Random;

import uk.ac.cam.cl.ss958.springboard.FriendMessage;
import uk.ac.cam.cl.ss958.toolkits.AsymmetricCryptoToolbox;
import uk.ac.cam.cl.ss958.toolkits.SerializableToolkit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// SINGLETON
public class HugglerDatabase extends SQLiteOpenHelper {
	private static final String TAG = "Huggler"; 
	
	private static HugglerDatabase instance = null;
	
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "Huggler";
 
    // Debug Properties table
    private static final String DEBUG_TABLE_NAME = "debug_properties";
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
    private static final String PROPERTIES_TABLE_NAME = "properties";
    private static SqlKeyValueTable properties_table;
    
    public enum Property {
    	HUGGLER_ID ("huggler_id"),
    	KEYPAIR ("keypair");
    	
    	private String name;
    	
    	public String getName() {
    		return name;
    	}
    	
    	Property(String name) {
    		this.name = name;
    	}
    }
    
    private static final String TABLE_MESSAGES = "chatmessages";
    private static SqlChatMessagesTable messages_table;
    
    private static final String TABLE_FRIENDS = "friends";
    private static SqlFriendsTable friends_table;
    
    private SQLiteDatabase db;
    
    private HugglerDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }
    
    public static void init(Context context) {
    	if(instance == null) {
    		instance = new HugglerDatabase(context);
    	} else {
    		Log.w(TAG, "Database initialized multiple times!");
    	}
    }
    
    public static HugglerDatabase get() {
    	if (instance != null) {
    		return instance;
    	} else {
    		Log.wtf(TAG, "HugglerDatabase get called before init!");
    		android.os.Process.killProcess(android.os.Process.myPid());
    		return null;
    	}
    }
    
 
    private boolean tables_initialized = false;
    
    public void initTables(SQLiteDatabase db) {
    	if (!tables_initialized) {
    		tables_initialized = true;
	        debug_table = new SqlKeyValueTable(db, DEBUG_TABLE_NAME);
	        properties_table = new SqlKeyValueTable(db, PROPERTIES_TABLE_NAME);
	        messages_table = new SqlChatMessagesTable(db, TABLE_MESSAGES);
	        friends_table = new SqlFriendsTable(db, TABLE_FRIENDS);
    	}
    }
    
    public SqlChatMessagesTable getMessageTable() {
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
        KeyPair kp = null;
        try {
        	kp = AsymmetricCryptoToolbox.generateKeypair();
        	String encodedKp = SerializableToolkit.toString(kp); 
            properties_table.insert(Property.KEYPAIR.getName(), encodedKp);        	
        } catch(Exception e) {
        	Log.wtf("Huggler", "Unable to generate keypair(" + e.getMessage() + ")");
        }
        
        messages_table.create();

        friends_table.create();
    }
    
    
    
    public KeyPair getMyKeyPair() {
    	try {
    		String encodedKp = readProperty(Property.KEYPAIR);
    		return (KeyPair)SerializableToolkit.fromString(encodedKp);
    	} catch(Exception e) {
    		Log.e("Huggler", "Cannot obtain keypair: (" + e.getMessage() + ")");
    		return null;
    	}
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
    
    public SqlFriendsTable getFriendsTable() {
    	return friends_table;
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	// add subsequent modificiations
    	// if oldVersion < 1 && newVersion >=1 create table debug_properties ...  
    }
}