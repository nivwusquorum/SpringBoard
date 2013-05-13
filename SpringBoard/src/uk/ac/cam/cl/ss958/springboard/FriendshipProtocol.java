package uk.ac.cam.cl.ss958.springboard;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import static junit.framework.Assert.*;

import uk.ac.cam.cl.ss958.springboard.content.DatabaseContentProvider;
import uk.ac.cam.cl.ss958.springboard.content.SpringboardSqlSchema;
import uk.ac.cam.cl.ss958.toolkits.SerializableToolkit;

public abstract class FriendshipProtocol {
	private static final int CHUNK_SIZE = 50;

	private int partsFinished;

	private static class ProfileInfo implements Serializable {
		final String username;
		final String organization;

		public ProfileInfo(String u, String o) {
			username = u;
			organization = o;
		}

	}

	private synchronized void maybeFinish() {
		++partsFinished;
		if (partsFinished == 2) {
			allDone();
		}
	}

	public static int byteArrayToInt(byte[] b) 
	{
		return   b[3] & 0xFF |
				(b[2] & 0xFF) << 8 |
				(b[1] & 0xFF) << 16 |
				(b[0] & 0xFF) << 24;
	}

	public static byte[] intToByteArray(int a)
	{
		return new byte[] {
				(byte) ((a >> 24) & 0xFF),
				(byte) ((a >> 16) & 0xFF),   
				(byte) ((a >> 8) & 0xFF),   
				(byte) (a & 0xFF)
		};
	}

	private byte [] mPiSerialized;

	private static final String TAG = "SpringBoard";

	private Context c;

	public FriendshipProtocol(Context c) {
		partsFinished = 0;
		try {
			showMessage("Attempting to establish Friendship");
			ProfileInfo pi = new ProfileInfo("Szymon Sidor", "Cambridge");

			mPiSerialized = SerializableToolkit.toBytes(pi);

			writeContent(intToByteArray(mPiSerialized.length));

			Log.d(TAG, "array size: " + mPiSerialized.length);

			byte [] b = new byte [CHUNK_SIZE];
			for(int i = 0; i<mPiSerialized.length; i+=CHUNK_SIZE) {
				int bytesToSend = Math.min(CHUNK_SIZE, mPiSerialized.length - i);
				if (bytesToSend != CHUNK_SIZE) {
					b = new byte[bytesToSend];
				}
				for(int j=0; j<CHUNK_SIZE && i+j <mPiSerialized.length; ++j) {
					b[j] = mPiSerialized[i+j];
				}
				writeContent(b);
			}

			maybeFinish();
		} catch (IOException e) {
			Log.d(TAG, "Exception while sending data:" +e.getMessage());
			for (StackTraceElement el : e.getStackTrace()) {
				Log.d(TAG, el.toString());
			}
			stop();
		}		
	}

	int totalByteCount = -1;
	int nextByte;

	byte [] result;

	protected void readContent(byte [] buff, int len) {
		if (totalByteCount == -1) {
			assertTrue(len >= 4);

			totalByteCount = byteArrayToInt(buff);
			Log.d(TAG, "received array size: " + totalByteCount);
			nextByte = 0;

			result = new byte[totalByteCount];

			if (len > 4) {
				readToResult(buff, 4, len);
				return;

			}
		}

		readToResult(buff, 0, len);

	}

	protected void readToResult(byte [] b, int s, int f) {
		for (int i=s; i<f && nextByte<totalByteCount; ++i) {
			result[nextByte++] = b[i];
		}
		if (nextByte == totalByteCount) 
			readingDone();
	}

	protected void readingDone() {
		try {
			ProfileInfo pi = (ProfileInfo)SerializableToolkit.fromBytes(result);
			showMessage("You are now friends with " + pi.username + " from " + pi.organization);
			maybeFinish();
		} catch (Exception e) {
			Log.d(TAG, "Exception while reading data:" +e.getMessage());
			for (StackTraceElement el : e.getStackTrace()) {
				Log.d(TAG, el.toString());
			}
			stop();
		}
	}


	public void stop() {
		showMessage("Friendship establishment Failed.");
	}

	protected void allDone() {
		showMessage("All done!");
	}

	protected abstract void writeContent(byte [] buff);

	protected abstract void showMessage(String m);


	public static void main(String [] args) throws Exception {
		ProfileInfo pi = new ProfileInfo("Szymon Sidor", "Cambridge");
		byte [] arr  = SerializableToolkit.toBytes(pi);
		System.out.println(arr.length);

	}

	private static Uri propertiesTableUri = 
			Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
					"/" + SpringboardSqlSchema.Strings.Properties.NAME);

	private ProfileInfo readMyProfile() {
		try {
			final ContentResolver cr = c.getContentResolver();

			String [] projection = { SpringboardSqlSchema.Strings.Properties.KEY_NAME,
					SpringboardSqlSchema.Strings.Properties.KEY_VALUE };


			String username = null;
			String organization = null;
			Bitmap picture = null;
			Cursor c = cr.query(propertiesTableUri, projection, null , null, null);
			c.moveToFirst();  
			while (!c.isAfterLast()) {
				if (SpringboardSqlSchema.Strings.Properties.P_USERNAME.equals(c.getString(0))) {
					username = c.getString(1);
				} else if (SpringboardSqlSchema.Strings.Properties.P_INSTITUTION.equals(c.getString(0))) {
					organization = c.getString(1);
				} else if (SpringboardSqlSchema.Strings.Properties.P_IMG_PATH.equals(c.getString(0))) {
					Uri uri = Uri.parse(c.getString(1));
					File image;
					image = new File(new URI(uri.toString()));

					BitmapFactory.Options opts = new BitmapFactory.Options();
					picture = BitmapFactory.decodeFile(image.getPath(), opts);     
				}
				c.moveToNext();
			}
			assertTrue(username != null && organization != null && picture != null);

		} catch (URISyntaxException e) {
			Log.d(TAG, "Error creating profile info,");
		}
		return null;
	}
}
