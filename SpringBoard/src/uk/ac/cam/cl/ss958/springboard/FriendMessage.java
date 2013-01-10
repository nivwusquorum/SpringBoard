package uk.ac.cam.cl.ss958.springboard;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import uk.ac.cam.cl.ss958.toolkits.AsymmetricCryptoToolbox;

import android.util.Log;

public class FriendMessage implements Serializable {
	String name;
	byte [] key;
	
	public FriendMessage(String n, PublicKey k) {
		name = n;
		key = k.getEncoded();
	}
	
	public String getName() {
		return name;
	}
	
	public PublicKey getKey() {
		try {
			return AsymmetricCryptoToolbox.decodePublicKey(key);
		} catch (Exception e) {
			Log.e("SpringBoard", "Cannot decode key (" + e.getMessage() + ")");
			return null;
		}
	}
}
