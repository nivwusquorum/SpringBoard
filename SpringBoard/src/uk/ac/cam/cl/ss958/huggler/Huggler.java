package uk.ac.cam.cl.ss958.huggler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.Property;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class Huggler extends Service {

	private static final String TAG = "Huggler";
	
	private boolean running;
	HugglerDatabase dbh;
	
	private Handler askingHandler;
	private Runnable runAsk;

	@Override
	public void onCreate() {
		/*
	    // Start up the thread running the service.  Note that we create a
	    // separate thread because the service normally runs in the process's
	    // main thread, which we don't want to block.  We also make it
	    // background priority so CPU-intensive work will not disrupt our UI.
	    HandlerThread thread = new HandlerThread("ServiceStartArguments",
	            Process.THREAD_PRIORITY_BACKGROUND);
	    thread.start();

	    // Get the HandlerThread's Looper and use it for our Handler 
	    mServiceLooper = thread.getLooper();
	    mServiceHandler = new ServiceHandler(mServiceLooper);
		 */
		running = false;
		askingHandler = new Handler();
		runAsk = new Runnable() {
			@Override
			public void run() {
				ask();
				askingHandler.postDelayed(this, 30000); // 30s
			}
		};
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(!running) {
			// concurrency bug very unlikely
			running = true;
			Toast.makeText(this, "Huggler service started. ", Toast.LENGTH_SHORT).show();
			dbh = new HugglerDatabase(getApplicationContext());
			initialize();
		}
		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	private void initialize() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				setUpServer();
				startAnswering();
			}
		}).start();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				setUpJmdns();
				startAsking();
			}
		}).start();

	}

	private void setUpServer() {
		try {
			// Creates server socket on some free port.
			server = new ServerSocket(0);
			Log.d(TAG, "Started server on port " + server.getLocalPort());
		} catch (IOException e) {
			Log.wtf(TAG, "Cannot create ServerSocket.");
		}
	}

	private void startAnswering() {
		Log.d(TAG, "Server starts to answer queries");
		try {
			while(true) {
				Socket clientSocket = server.accept();
				Log.d(TAG, "Incoming connection");
				handleClient(clientSocket);
			}
		} catch(Exception e) {
			// TODO Restart server when disconnected
			Log.e(TAG, " Disconnected... ");
		}	
	}
	
	private ServiceListener jmdnsListener;
	private String jmdnsType = "_huggler._tcp.local.";
	private JmDNS jmdns = null;
	private ServiceInfo serviceInfo;
	private MulticastLock multicastLock; 
		
	private void setUpJmdns() {
		android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		multicastLock = wifi.createMulticastLock("mylockthereturn");
		multicastLock.setReferenceCounted(true);
		multicastLock.acquire();
		try {
            jmdns = JmDNS.create();
            jmdns.addServiceListener(jmdnsType, jmdnsListener = new ServiceListener() {
                @Override
                public void serviceResolved(ServiceEvent ev) {
                	Log.d(TAG, "==>  Listener: resolved " + ev.getName());
                }

                @Override
                public void serviceRemoved(ServiceEvent ev) {
                }

                @Override
                public void serviceAdded(ServiceEvent ev) {
                    // Required to force serviceResolved to be called again (after the first search)
                    jmdns.requestServiceInfo(ev.getType(), ev.getName(), 1);
                }
            });
            
            jmdns.registerServiceType(jmdnsType);
			String huggler_id = dbh.readProperty(Property.HUGGLER_ID);
			serviceInfo = ServiceInfo.create(jmdnsType, huggler_id, server.getLocalPort(), " Huggler service for opportunistic communication.");

			jmdns.registerService(serviceInfo);
			Log.d(TAG, "JMDNS service registered on ip " + jmdns.getInterface());
		} catch (IOException e) {
			Log.e(TAG, "Error creating JMDNS service (" + e.getMessage() + ")");
			e.printStackTrace();
		}

	}
	
	
	private void startAsking() {
		runAsk.run();
	}
	
	
	
	private void ask() {
		Log.d(TAG, "Started asking");
		String people = "";
		for(ServiceInfo si : jmdns.list(jmdnsType, 6000)) { // timeout: 6s
			Log.d(TAG, " Trying to connect to " + si.getName());
			if (si.getName().equals(dbh.readProperty(Property.HUGGLER_ID)))
				continue;
			// TODO: What if there are multiple host address?
			// When? Why?
			try {
				Log.d(TAG, "Host has " + si.getInet4Addresses().length + " addresses. ");
				if(si.getInet4Addresses().length == 0) continue;
				Log.d(TAG, "Trying to connect to " + si.getInet4Addresses()[0] + " on port " + si.getPort());
				Socket s = new Socket(si.getInet4Addresses()[0], si.getPort());
				ObjectInputStream reader = new ObjectInputStream(s.getInputStream());
				Object message = reader.readObject();
				if(message instanceof String) {
					people = people + (String)message + ", ";
				}
				s.close();
			} catch (Exception e) {
				Log.w(TAG, "Cannot communicate with discovered peer (" +e.getMessage() + "). ");
				
				for (StackTraceElement el : e.getStackTrace()) {				
					Log.w(TAG, el.toString());
				}
			}
		}
		Log.d(TAG, "Communicated with "+people+"wow! ");
	}
	

	private void handleClient(Socket clientSocket) {
		final Socket s = clientSocket;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// TODO add timeout.
					// ObjectInputStream reader = new ObjectInputStream(s.getInputStream());
					ObjectOutputStream writer = new ObjectOutputStream(s.getOutputStream());

					// TODO: read request
					//Object message = reader.readObject();
					Log.e(TAG, "Database!");
					String myname = dbh.readProperty(Property.HUGGLER_ID);
					Log.d(TAG, "Preparing to write object " + myname);
					writer.writeObject(myname);
					Log.d(TAG, "Object written. ");
					s.close();
				} catch(Exception e) {
					Log.w(TAG, "Unsuccesful exchange");
				}
			}
		}).start();
	}

	private ServerSocket server;

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
    	if (jmdns != null) {
            if (jmdnsListener != null) {
                jmdns.removeServiceListener(jmdnsType, jmdnsListener);
                jmdnsListener = null;
            }
            jmdns.unregisterAllServices();
            try {
                jmdns.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            jmdns = null;
    	}
        multicastLock.release();
		
		dbh.close();
		if(running) {			
			running = false;
			Toast.makeText(this, "Huggler service stopped.", Toast.LENGTH_SHORT).show(); 
		}
	}
}