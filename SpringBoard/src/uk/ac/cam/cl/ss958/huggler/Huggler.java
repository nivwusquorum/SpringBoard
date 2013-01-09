package uk.ac.cam.cl.ss958.huggler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.Property;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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

	private HugglerProtocol protocol;
	@Override
	public void onCreate() {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Stack", "false");

		running = false;
		askingHandler = new Handler();
		runAsk = new Runnable() {
			@Override
			public void run() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						ask();
					}
				}).start();
				askingHandler.postDelayed(this, 
						60 * 1000 * HugglerConfig.UPDATE_INTERVAL_M);
			}
		};
		protocol = new HugglerSpringBoardProtocol(this);
	}

    public HugglerDatabase getDb() {
    	return dbh;
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
		Thread server = new Thread(new Runnable() {
			@Override
			public void run() {
				setUpServer();
				startAnswering();
			}
		});
		
		Thread discovery = new Thread(new Runnable() {
			@Override
			public void run() {
				setUpJmdns();
				startAsking();
			}
		});
		// TODO set priorites to background
		server.setPriority(Thread.MIN_PRIORITY);
		discovery.setPriority(Thread.MIN_PRIORITY);
		server.start();
		discovery.start();
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
		for(ServiceInfo si : jmdns.list(jmdnsType, 6000)) { // timeout: 6s
			Log.d(TAG, " Trying to connect to " + si.getName());
			if (si.getName().equals(dbh.readProperty(Property.HUGGLER_ID)))
				continue;
			try {
				// TODO: What if there are multiple host address?
				// When? Why?
				Log.d(TAG, "Host has " + si.getInet4Addresses().length + " v4 addresses. ");
				Log.d(TAG, "Host has " + si.getInet6Addresses().length + " v6 addresses. ");
				if(si.getInet6Addresses().length > 0) {
					Log.d(TAG, "Host IPv6 address is " + si.getInet6Addresses()[0].getHostAddress());
					Log.d(TAG, "Host IPv6 hostname is " + si.getInet6Addresses()[0].getCanonicalHostName());
					Log.d(TAG, "Host is linklocal " + si.getInet6Addresses()[0].isLinkLocalAddress());
				}
				if(si.getInet4Addresses().length == 0) continue;
				Log.d(TAG, "Trying to connect to " + si.getInet4Addresses()[0] + " on port " + si.getPort());
				Socket s = new Socket(si.getInet4Addresses()[0], si.getPort());

				String clientName = determineAndValidateReceiver(s, true);
				
				protocol.askClient(s, clientName); 
			} catch (Throwable e) {
				Log.w(TAG, "Cannot communicate with discovered peer (" +e.getMessage() + "). ");
				for (StackTraceElement el : e.getStackTrace()) {				
					Log.w(TAG, el.toString());
				}
			}
		}
		Log.d(TAG, "Done asking");
	}
	

	public String getUserName() {
		return dbh.readProperty(Property.HUGGLER_ID);
	}
	
	private HugglerIntroMessage receiveIntroMessage(Socket clientSocket) {
		try {
			ObjectInputStream reader = new ObjectInputStream(clientSocket.getInputStream());
			
			Object message = reader.readObject();
			HugglerIntroMessage introMessage;
			if(message instanceof HugglerIntroMessage) {
				introMessage = (HugglerIntroMessage)message;
			} else {
				throw new Exception("Intro message not received");
			}
			return introMessage;
		} catch(Exception e) {
			Log.d(TAG, "Cannot receive intro Message (" + e.getMessage()+")");
			return null;
		}
	}
	
	private boolean sendIntroMessage(Socket clientSocket) {
		try {
			ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
	
			HugglerIntroMessage introMessage =
					new HugglerIntroMessage(protocol.getName(),
											getUserName());
			writer.writeObject(introMessage);
			return true;
		} catch(Exception e) {
			Log.d(TAG, "Cannot send intro Message (" + e.getMessage()+")");
			return false;
		}
	}
	
	private String determineAndValidateReceiver(Socket clientSocket, boolean initiating) {
		HugglerIntroMessage introMessage = null;
		boolean messageSent = false;
		if(initiating) {
			messageSent = sendIntroMessage(clientSocket);
			if(messageSent) {
				introMessage = receiveIntroMessage(clientSocket);
			}
		} else {
			introMessage = receiveIntroMessage(clientSocket);
			if (introMessage != null) {
				sendIntroMessage(clientSocket);
			}
		}
		if (!messageSent || introMessage == null) 
			return null;
		if (!introMessage.getProtocol().equals(protocol.getName()))
			return null;
		return introMessage.getName();
	}
	
	private void handleClient(Socket clientSocket) {
		final Socket s = clientSocket;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String clientName = determineAndValidateReceiver(s, false);
					protocol.answerClient(s, clientName);
				} catch(Exception e) {
					Log.d(TAG, "Unsuccesful exchange ("+ e.getMessage()+")");
					for (StackTraceElement el : e.getStackTrace()) {				
						Log.w(TAG, el.toString());
					}
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