package uk.ac.cam.cl.ss958.huggler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.Property;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class Huggler {

	private static final String TAG = "Huggler";
	
	List<HugglerExtension> extensions;
	
	public Huggler(Context context) {
		HugglerDatabase.init(context);
		extensions = new ArrayList<HugglerExtension>();
	}
	
	public void addExtension(HugglerExtension e) {
		extensions.add(e);
	}


	public void start() {
		for(HugglerExtension e : extensions) {
			e.create();
		}
		for(HugglerExtension e : extensions) {
			e.start();
		}
	}

	public void destroy() {
		for(HugglerExtension e : extensions) {
			e.stop();
		}
		for(HugglerExtension e : extensions) {
			e.destroy();
		}
		HugglerDatabase.closeAll();
	}
}