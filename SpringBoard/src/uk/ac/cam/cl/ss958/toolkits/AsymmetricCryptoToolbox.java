package uk.ac.cam.cl.ss958.toolkits;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import biz.source_code.base64Coder.Base64Coder;
import android.util.Log;

public class AsymmetricCryptoToolbox {
	private static final String xform = "RSA/ECB/PKCS1PADDING"; // "RSA/NONE/PKCS1PADDING";

	
	public static final String TAG = "Springboard";

	public static PublicKey decodePublicKey(byte [] key) throws Exception {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec KeySpec = new X509EncodedKeySpec(key);
		PublicKey pubKey = (PublicKey)keyFactory.generatePublic(KeySpec);
		return pubKey;
	}
	
	public static byte[] decrypt(byte[] inpBytes, PublicKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(inpBytes);
	}
		
	public static byte[] encrypt(byte[] inpBytes, PrivateKey key) throws Exception{
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(inpBytes);
	}
	
	public static byte[] encrypt(String s, PrivateKey key) throws Exception {
		return encrypt(s.getBytes(), key);
	}
	
	public static String decryptToString(byte[] inpBytes, PublicKey key) throws Exception{
		return new String(decrypt(inpBytes, key));
	}


	public static KeyPair generateKeypair() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048); // 512 is the keysize.
		return kpg.generateKeyPair();
	}
	
	public static void main(String[] unused) throws Exception {
		// Generate a key-pair
/*

		byte[] dataBytes =
				"J2EE Security for Servlets, EJBs and Web Services".getBytes();

		byte[] encBytes = encrypt(dataBytes, pubk, xform);
		byte[] decBytes = decrypt(encBytes, prvk, xform);

		byte[] decBytes = decrypt(encBytes, prvk, xform);
		boolean expected = java.util.Arrays.equals(dataBytes, decBytes);
		System.out.println("Test " + (expected ? "SUCCEEDED!" : "FAILED!")); */
	}
}
