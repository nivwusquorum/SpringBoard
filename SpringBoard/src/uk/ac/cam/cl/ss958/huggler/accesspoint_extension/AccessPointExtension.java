package uk.ac.cam.cl.ss958.huggler.accesspoint_extension;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.content.Context;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.util.Log;
import uk.ac.cam.cl.ss958.huggler.HugglerConfig;
import uk.ac.cam.cl.ss958.huggler.HugglerExtension;
import uk.ac.cam.cl.ss958.huggler.HugglerIntroMessage;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.Property;
import uk.ac.cam.cl.ss958.huggler.HugglerProtocol;
import uk.ac.cam.cl.ss958.springboard_huggler.HugglerSpringBoardProtocol;

public class AccessPointExtension extends HugglerExtension {

	private static final String TAG = "Huggler";
	
	private Handler askingHandler;
	private Thread serverThread;
	private Thread clientThread;
	private boolean maybeStopped = true;
	
	private ServerSocket server;
	
	private ServiceListener jmdnsListener;
	private String jmdnsType = "_huggler._tcp.local.";
	private JmDNS jmdns = null;
	private ServiceInfo serviceInfo;
	private MulticastLock multicastLock;
	
	class NoProtocolException extends Exception {
	}
	
	android.net.wifi.WifiManager wifi = null;
	
	private void assertProtocol() throws NoProtocolException {
		if (protocol == null) {
			throw new NoProtocolException();
		}
	}
	
	public AccessPointExtension(Context context) {
		wifi = (android.net.wifi.WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
		
	}
	
	@Override
	public void create() {
		askingHandler = new Handler();
		
		clientThread = new Thread() {
			@Override
			public void run() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							clientAction();
						} catch(NoProtocolException e) {
							Log.e(TAG, "AccessPoint: you must set protocol first!");
						}
					}
				}).start();
				if(!maybeStopped) {
					askingHandler.postDelayed(this, 
							60 * 1000 * HugglerConfig.ACCESS_POINT_UPDATE_FREQUENCY_M);
				}
			}
		};
		
		serverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				serverAction();
			}
		});
		
		// TODO set priorities to background
		serverThread.setPriority(Thread.MIN_PRIORITY);
		clientThread.setPriority(Thread.MIN_PRIORITY);
	}	

	@Override
	public void start() {
		maybeStopped = false;
		Thread networkJobs = new Thread() {
			@Override
			public void run() {
				initServer();
				initClient();
			}
		};
		networkJobs.start();
		try {
			networkJobs.join();
		} catch (InterruptedException e) {
			// TODO: figure out what to do here
		}
		Log.d(TAG, "Client/Server successfully started.");
		serverThread.start();
		clientThread.start();
	}
	
	private void initServer() {
		try {
			// Creates server socket on some free port.
			server = new ServerSocket(0);
			server.setSoTimeout(5000);
			Log.d(TAG, "Started server on port " + server.getLocalPort());
		} catch (IOException e) {
			Log.wtf(TAG, "Cannot create ServerSocket.");
		}
	}
	
	
	private void serverAction() {
		Log.d(TAG, "Server starts to answer queries");
		try {
			while(!maybeStopped) {
				try {
					Socket clientSocket = server.accept();
					Log.d(TAG, "Incoming connection");
					handleClient(clientSocket);
				} catch(SocketTimeoutException e) {
					// Intentionally ignore.
					// Server timeout.
				}
			}
		} catch(Exception e) {
			// TODO Restart server when disconnected
			Log.e(TAG, " Disconnected... ");
		}	
	}
	
	private void handleClient(Socket clientSocket) throws NoProtocolException {
		assertProtocol();
		
		final Socket s = clientSocket;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					protocol.answerClient(s);
				} catch(Exception e) {
					Log.d(TAG, "Unsuccesful exchange ("+ e.getMessage()+")");
					for (StackTraceElement el : e.getStackTrace()) {				
						Log.w(TAG, el.toString());
					}
				}
			}
		}).start();
	}
	
	private void stopServer() {
		try {
			server.close();
		} catch(Exception e) {
			Log.wtf(TAG, "Cannot close server");
		}
	}

	private void initClient() {
		multicastLock = wifi.createMulticastLock("mylockthereturn");
		multicastLock.setReferenceCounted(true);
		multicastLock.acquire();
		try {

			// handle not being connected to wifi you idiot!
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
			HugglerDatabase dbh = HugglerDatabase.get();
			String huggler_id = dbh.readProperty(Property.HUGGLER_ID);
			serviceInfo = ServiceInfo.create(jmdnsType, huggler_id, server.getLocalPort(), " Huggler service for opportunistic communication.");

			jmdns.registerService(serviceInfo);
			Log.d(TAG, "JMDNS service registered on ip " + jmdns.getInterface());
		} catch (IOException e) {
			Log.e(TAG, "Error creating JMDNS service (" + e.getMessage() + ")");
			e.printStackTrace();
		}

	}



	private void clientAction() throws NoProtocolException {
		assertProtocol();
		
		Log.d(TAG, "Started asking");
		HugglerDatabase dbh = HugglerDatabase.get();
		String my_name = dbh.readProperty(Property.HUGGLER_ID);
		for(ServiceInfo si : jmdns.list(jmdnsType, 6000)) { // timeout: 6s
			if (si.getName().equals(my_name))
				continue;
			Log.d(TAG, " Trying to connect to " + si.getName());
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

				protocol.askClient(s); 
			} catch (Throwable e) {
				Log.w(TAG, "Cannot communicate with discovered peer (" +e.getMessage() + "). ");
				for (StackTraceElement el : e.getStackTrace()) {				
					Log.w(TAG, el.toString());
				}
			}
		}
		Log.d(TAG, "Done asking");
	}
	
	
	private void stopClient() {
		if (jmdns != null) {
			if (jmdnsListener != null) {
				jmdns.removeServiceListener(jmdnsType, jmdnsListener);
				jmdnsListener = null;
			}
			jmdns.unregisterAllServices();
			try {
				jmdns.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			jmdns = null;
		}
		multicastLock.release();
	}

	
	@Override
	public void stop() {
		try {
			clientThread.join();
			serverThread.join();
		} catch (InterruptedException e) {
			// TODO Find out what happens here
			Log.wtf(TAG, "Interrupted: " + e.getMessage());
		}
		stopServer();
		stopClient();
	}

	@Override
	public void destroy() {

	}
	
}
