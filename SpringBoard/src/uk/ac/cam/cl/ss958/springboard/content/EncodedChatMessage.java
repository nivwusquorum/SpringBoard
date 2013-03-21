package uk.ac.cam.cl.ss958.springboard.content;

import java.io.IOException;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import android.util.Log;

import uk.ac.cam.cl.ss958.toolkits.AsymmetricCryptoToolbox;
import uk.ac.cam.cl.ss958.toolkits.SerializableToolkit;

public class EncodedChatMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	
	String user;
	byte [] encodedMessage;
	
	public EncodedChatMessage(ChatMessage cm, PrivateKey key) throws Exception {
		byte [] serializedCM = SerializableToolkit.toBytes(cm);
		encodedMessage = AsymmetricCryptoToolbox.encrypt(serializedCM, key);
		user = cm.getUser();
	}
	
	public static void testEncryption(ChatMessage cm, KeyPair kp) {
		try {
			byte [] serializedCM = SerializableToolkit.toBytes(cm);
			byte [] encodedMessage = AsymmetricCryptoToolbox.encrypt(serializedCM, kp.getPrivate());
			byte [] decodedMessage = AsymmetricCryptoToolbox.decrypt(encodedMessage, kp.getPublic());
			ChatMessage lastcm = (ChatMessage)SerializableToolkit.fromBytes(decodedMessage);
			Log.d("Huggler", "TEST SUCCESS: " + lastcm.getUser());
		} catch(Exception e) {
			Log.e("Huggler","TEST FAILED: " + e.getMessage());
			for(StackTraceElement el : e.getStackTrace()) {
				Log.e("Huggler", el.toString());
			}
		}
	}
	
	public String getUser() {
		return user;
	}
	
	public ChatMessage getChatMessage(PublicKey key) throws Exception {
		byte [] decodedMessage = AsymmetricCryptoToolbox.decrypt(encodedMessage, key);
		return (ChatMessage)SerializableToolkit.fromBytes(decodedMessage);
	}
}
