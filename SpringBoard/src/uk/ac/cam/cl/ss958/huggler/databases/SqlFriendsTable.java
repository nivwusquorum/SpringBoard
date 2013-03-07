package uk.ac.cam.cl.ss958.huggler.databases;

import java.security.PublicKey;

import uk.ac.cam.cl.ss958.springboard.FriendMessage;
import uk.ac.cam.cl.ss958.toolkits.SerializableToolkit;
import android.database.sqlite.SQLiteDatabase;

public class SqlFriendsTable extends SqlKeyValueTable {

	SqlFriendsTable(SQLiteDatabase db, String name) {
		super(db, name);
		// TODO Auto-generated constructor stub
	}
	
	public boolean addFriend(FriendMessage fm) {
    	try {
    		return insert(fm.getName(), 
    			SerializableToolkit.toString(fm.getKey()));
    	} catch (Exception e) {
    		return false;
    	}
    }
    
    public PublicKey getKeyForFriend(String friend) {
    	try {
	    	String serializedKey = get(friend);
	    	if (serializedKey == null)
	    		return null;
	    	PublicKey key = 
	    			(PublicKey)SerializableToolkit.fromString(serializedKey);
	    	return key;
    	} catch(Exception e) {
    		return null;
    	}
    }

}
