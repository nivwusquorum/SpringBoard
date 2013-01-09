package uk.ac.cam.cl.ss958.springboard;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.zxing.integration.android.*;

import uk.ac.cam.cl.ss958.huggler.ChatMessage;
import uk.ac.cam.cl.ss958.huggler.Huggler;
import uk.ac.cam.cl.ss958.huggler.HugglerConfig;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.DebugProperty;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.Property;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class MainActivity extends Activity {
	static final String TAG = "SpringBoard";
	
	HugglerDatabase dbh;

	IntentIntegrator integrator;
	
	ViewLoader currentView = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent service_intent = new Intent(this, Huggler.class);
		startService(service_intent);	
		
		dbh = new HugglerDatabase(this);
		integrator = new IntentIntegrator(this);
		
		loadView(ViewToLoad.MAIN_VIEW);
	}
	
	public enum ViewToLoad {
		MAIN_VIEW(MainViewLoader.class),
		QRCODE_VIEW(QrCodeViewLoader.class);
		
		Class loader;
		
		ViewToLoad(Class c) {
			loader = c;
		}
		
		public Class getLoader() {
			return loader;
		}
		
	}
	
	Map<ViewToLoad,ViewLoader> preallocatedView;
	
	public void loadView(ViewToLoad view) {
		if (preallocatedView == null) {
			preallocatedView = new HashMap<ViewToLoad,ViewLoader>();
		}
		
		if (!preallocatedView.containsKey(view)) {
			Constructor [] constructors = view.getLoader().getConstructors();
			Object [] args = { this };
			try {
				preallocatedView.put(view, (ViewLoader)constructors[0].newInstance(args));
			} catch(Exception e) {
				Log.e(TAG, "Unable to instantiate ViewLoader class.");
			}
		}
		
		if(currentView != null) {
			currentView.relieve();
		}
		
		currentView = preallocatedView.get(view);
		currentView.load();
		currentView.resume();
	}
	
	public void initiateBarcodeScanForFriend() {
		integrator.initiateScan();
	}
	
	public HugglerDatabase getDbh() {
		return dbh;
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// TODO what if this is different result
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			Log.e(TAG, "Scanned barcode: " + scanResult.getContents());
		} else {
			Log.e(TAG, "Failed to obtain QR code.");
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		currentView.load();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		currentView.resume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		currentView.pause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		currentView.relieve();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		dbh.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
