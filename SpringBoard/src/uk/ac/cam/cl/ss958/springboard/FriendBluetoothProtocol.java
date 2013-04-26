package uk.ac.cam.cl.ss958.springboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;


// TODO: add all this logic preventing multiple communication at once.


public class FriendBluetoothProtocol {
	private static final String TAG = "SpringBoard";

	private boolean mServerRunning;
	private boolean mServerStaph = false;
	
	private BluetoothAdapter mAdapter;
	private AddFriendActivity context;
	
	private UUID springboardFriendProtocolUUID =
			//UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
			UUID.fromString("fd4ec150-6a46-4054-a215-5df0876be2a2");
	private String springboardFriendProtocolName = 
			"springboardFriendProtocol";
	private BluetoothServerSocket mServerSocket;
	
	public FriendBluetoothProtocol(BluetoothAdapter adapter, AddFriendActivity context) {
		mServerRunning = false;
		this.mAdapter = adapter;
		this.context = context;
	}
	
	// server only accepts one connection at a time.
	public boolean startServer() {
		if (!mServerRunning) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						// Not too secure but will do.
						if (mServerRunning) {
							return;
						}
						mServerRunning = true;
						
						mServerSocket = 
								mAdapter.listenUsingRfcommWithServiceRecord(
										springboardFriendProtocolName,
										springboardFriendProtocolUUID);
						
						BluetoothSocket connection;
						
						while (!mServerStaph) {
							try {
								connection = mServerSocket.accept();
								Log.d(TAG, "Incoming bluetooth connection!!!!");
								
								mAdapter.cancelDiscovery();
								Log.d(TAG, "Blue reception started!");
								String msg = readDataWithString(connection);
								Log.d(TAG, "Blue reception ended: " + msg);
								Toast.makeText(context, msg , Toast.LENGTH_LONG).show();

								connection.close();
							} catch (Exception e) {
								if(!("Connection timed out".equals(e.getMessage())) &&
								   !("Try again".equals(e.getMessage()))		) {
									Log.d(TAG, "BlueServ exception: " + e.getMessage());
									for(StackTraceElement el : e.getStackTrace()) {
										Log.d(TAG, el.toString());
									}
								}
							}
						}
						
					} catch(Exception e) {
						Log.d(TAG, "BlueServ exception: " + e.getMessage());
					}
					Log.d(TAG, "Blue server stopped.");
					mServerRunning = false;
					mServerStaph = false;
				}				
			}).start();
			
		} 
		return mServerRunning;
	}
	
	public boolean stopServer() {
		if (mServerRunning) {
			try {
				mServerStaph = true;
				mServerSocket.close();
				
			} catch (Exception e) {
			}
		} 
		return !mServerRunning;
	}
	
	public boolean connectTo(final BluetoothDevice bd) {
		mAdapter.cancelDiscovery();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d(TAG, "BlueClient starting send!");
					mAdapter.cancelDiscovery();
					mServerStaph = true;
					mServerSocket.close();
					//BluetoothSocket connection = 
					//		bd.createRfcommSocketToServiceRecord(springboardFriendProtocolUUID);
					Method m = bd.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
					BluetoothSocket connection = (BluetoothSocket) m.invoke(bd, 1);
					connection.connect();
					sendDataWithString(connection, "yo!");
					connection.close(); 
					Log.d(TAG, "BlueClient ending send!");
				} catch (Exception e) {
					Log.d(TAG, "BlueClient exception: " + e.getMessage());
					for(StackTraceElement el : e.getStackTrace()) {
						Log.d(TAG, el.toString());
					}
				}
			}
		}).start();
		return false;
					
	}
	
	public void sendDataWithString(BluetoothSocket s, String message) throws IOException {
        if (message != null) {
        	PrintWriter out =
					new PrintWriter(s.getOutputStream());
            out.write(message);
            out.flush();
        }
    }
	
	public String readDataWithString(BluetoothSocket s) throws IOException {
        final int BUFFER_SIZE = 2048;
        String message = "";
        int charsRead = 0;
        
        BufferedReader in = 
				new BufferedReader(new InputStreamReader(s.getInputStream()));
		
        
        char[] buffer = new char[BUFFER_SIZE];
        while (!(s.getInputStream().available() > 0)) {
	        while ((charsRead = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
	            message += new String(buffer).substring(0, charsRead);
	        }
        }

        return message;

    }	
	
	

}
