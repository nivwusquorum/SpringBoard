package uk.ac.cam.cl.ss958.springboard;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.SlidingDrawer;
import static junit.framework.Assert.*;

import uk.ac.cam.cl.ss958.springboard.content.DatabaseContentProvider;
import uk.ac.cam.cl.ss958.springboard.content.SpringboardSqlSchema;
import uk.ac.cam.cl.ss958.toolkits.SerializableToolkit;

public abstract class FriendshipProtocol {
	private static final int CHUNK_SIZE = 5000;

	private int partsFinished;

	private static class ProfileInfo implements Serializable, Parcelable {
		final String username;
		final String organization;
		final Bitmap picture;

		public ProfileInfo(String u, String o, Bitmap b) {
			Log.d(TAG, "Created new profile info of user: " + u);
			username = u;
			organization = o;
			picture = b;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel in, int flags) {
			in.writeString(username);
			in.writeString(organization);

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			picture.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] byteArray = stream.toByteArray();
			in.writeInt(byteArray.length);
			in.writeByteArray(byteArray);
		}

		public static final Parcelable.Creator<ProfileInfo> CREATOR
		= new Parcelable.Creator<ProfileInfo>() {
			public ProfileInfo createFromParcel(Parcel in) {
				return new ProfileInfo(in);
			}

			public ProfileInfo[] newArray(int size) {
				return new ProfileInfo[size];
			}
		};

		private ProfileInfo(Parcel in) {
			in.setDataPosition(0);
			this.username = in.readString();
			Log.d(TAG, "Read username from parcel: " + this.username);
			this.organization = in.readString();

			int arrlength = in.readInt();

			byte [] arr = new byte[arrlength];
			in.readByteArray(arr);
			picture = BitmapFactory.decodeByteArray(arr, 0, arr.length);
		}
	}

	private synchronized void maybeFinish() {
		++partsFinished;
		if (partsFinished == 2) {
				// sometims can disconnect before the other end finishes 
				// receiving data and that causes BT to go mad.
				try {
					wait(500);
				} catch (InterruptedException e) {}
			
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

	public FriendshipProtocol(Context con) {
		c = con;
		partsFinished = 0;
		// totalByteCount = -1;
		showMessage("Attempting to establish Friendship");

		Thread sender = new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					ProfileInfo pi = readMyProfile();
					Parcel parcel = Parcel.obtain();
					pi.writeToParcel(parcel, 0);

					mPiSerialized = parcel.marshall();


					Parcel res = Parcel.obtain();
					res.unmarshall(mPiSerialized, 0, mPiSerialized.length);
					ProfileInfo pix = new ProfileInfo(res);

					synchronized(this) {
						wait(50);
					}
					writeContent(intToByteArray(mPiSerialized.length));			


					for(int i = 0; i<mPiSerialized.length; i+=CHUNK_SIZE) {
						try {
							synchronized(this) {
								wait(50);
							}
						} catch(Exception e) { }
						int bytesToSend = Math.min(CHUNK_SIZE, mPiSerialized.length - i);
						byte [] b = new byte [bytesToSend];
						for(int j=0; j<bytesToSend; ++j) {
							b[j] = mPiSerialized[i+j];
						}
						writeContent(b);
					}

					maybeFinish();
				} catch (Exception e) {
					Log.d(TAG, "Exception while sending data:" +e.getMessage());
					for (StackTraceElement el : e.getStackTrace()) {
						Log.d(TAG, el.toString());
					}
					stop();
				}	
			}
		});
		sender.start();

		}

		int totalByteCount = -1;
		int nextByte;

		byte [] result;

		protected void readContent(byte [] buff, int len) {
			for (int i=0; i<len; ++i) {
				logReadByte(buff[i]);
			}

			if (totalByteCount == -1) {
				assertTrue(len >= 4);

				totalByteCount = byteArrayToInt(buff);

				nextByte = 0;

				if (totalByteCount < 0 || totalByteCount > 4000000) {
					Log.d(TAG, "Wrong array size: " + totalByteCount);
					stop();
					return;
				}
				result = new byte[totalByteCount];

				if (len > 4) {
					readToResult(buff, 4, len);

				}
				return;
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

		private ProfileInfo received;
		
		public String getName() {
			assertTrue(received != null);
			return received.username;
		}
		
		protected void readingDone() {
			try {
				Parcel res = Parcel.obtain();
				res.unmarshall(result, 0, totalByteCount);
				ProfileInfo pi = new ProfileInfo(res);
				received = pi;
				if (!saveProfile(pi)) {
					stop();
					return;
				}
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
		
		private static Uri friendsTableUri = 
				Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
						"/" + SpringboardSqlSchema.Strings.Friends.NAME);

		
		private Uri storeImage(Bitmap image, String name) {
			File file = c.getFileStreamPath(name + ".png");
			if (file.exists())
				file.delete();
		    try {
		        FileOutputStream fos = new FileOutputStream(file);
		        image.compress(Bitmap.CompressFormat.PNG, 90, fos);
		        fos.close();
		    } catch (FileNotFoundException e) {
		        Log.d(TAG, "File not found: " + e.getMessage());
		        return null;
		    } catch (IOException e) {
		        Log.d(TAG, "Error accessing file: " + e.getMessage());
		        return null;
		    }  
		    return Uri.fromFile(file);
		}
		
		private boolean saveProfile(ProfileInfo pi) {
			
			
			final ContentResolver cr = c.getContentResolver();
			
			
			String [] projection = { SpringboardSqlSchema.Strings.Friends.KEY_NAME };
			
			String selection = SpringboardSqlSchema.Strings.Friends.KEY_NAME +" =  ?";
			String [] selectionArgs = { pi.username };

			Cursor c = cr.query(friendsTableUri, projection, selection , selectionArgs, null);
			if (c.getCount() > 0) {
				showMessage(pi.username + " is already a Friend.");
				return false;
			}
			
			// TODO: change filename to one based on unique id.
			
			Uri uri = storeImage(pi.picture, pi.username);
			if (uri == null) {
				showMessage("Failed to save profile");
				Log.d(TAG, "Problem saving image.");
				return false;
			}
			ContentValues cv = new ContentValues();
			cv.put(SpringboardSqlSchema.Strings.Friends.KEY_NAME, pi.username);
			cv.put(SpringboardSqlSchema.Strings.Friends.KEY_ORGANIZATION, pi.organization);
			cv.put(SpringboardSqlSchema.Strings.Friends.KEY_IMAGE, uri.toString());
			cr.insert(friendsTableUri, cv);
			
			return true;
		}

		private ArrayList<Byte> sent;
		private ArrayList<Byte> read;

		private void logSentByte(byte x)  {
			if (sent == null) {
				sent = new ArrayList<Byte>();
			}
			sent.add(x);
		}

		private void logReadByte(byte x)  {
			if (read == null) {
				read = new ArrayList<Byte>();
			}
			read.add(x);
		}

		private void logCommunication() {
			synchronized (this) {
				if (sent != null || read != null) Log.d(TAG, "Logging communication.");
				if (sent != null) {
					String x = "";
					for (Byte b : sent) {
						x +="" + b + " ";
					}
					Log.d (TAG, "Data sent: " + x);
					sent = null;
				}

				if (read != null) {
					String y = "";
					for (Byte b : read) {
						y +="" + b + " ";
					}
					Log.d (TAG, "Data read: " + y);
					read = null;
				}
			}
		}

		boolean wasError = true;
		public void stop() {
			if (wasError) {
				showMessage("Friendship establishment Failed.");
				logCommunication();
			}
		}

		protected void allDone() {
			wasError = false;
			showMessage("All done!");
			stop();
		}

		protected void writeContent(byte [] buff) {
			for(int i=0; i<buff.length; ++i) {
				logSentByte(buff[i]);
			}
		}

		protected abstract void showMessage(String m);


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
				return new ProfileInfo(username, organization, picture);
			} catch (URISyntaxException e) {
				Log.d(TAG, "Error creating profile info,");
				stop();
				return null;
			}
		}
	}
