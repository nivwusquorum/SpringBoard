package uk.ac.cam.cl.ss958.springboard;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actionbarsherlock.app.SherlockActivity;
import com.google.zxing.integration.android.*;

import uk.ac.cam.cl.ss958.huggler.Huggler;
import uk.ac.cam.cl.ss958.huggler.HugglerConfig;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.DebugProperty;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.Property;
import uk.ac.cam.cl.ss958.springboard.content.ChatMessage;
import uk.ac.cam.cl.ss958.springboard_huggler.SpringBoardHugglerService;
import uk.ac.cam.cl.ss958.toolkits.SerializableToolkit;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class MainActivity extends SherlockActivity {
	static final String TAG = "SpringBoard";
	
	HugglerDatabase dbh;

	IntentIntegrator integrator;
	
	ViewLoader currentView = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		HugglerDatabase.init(this);
		dbh = HugglerDatabase.get();
		
		// Make sure service is running
		Intent service_intent = new Intent(this, SpringBoardHugglerService.class);
		startService(service_intent);	

		
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
	
	Handler messageHandler;
	
	public void showMessage(String message) {
		final String mes = message;
		if(messageHandler == null) { 
			messageHandler = new Handler();
		}
		final Activity self = this;
		Log.d(TAG, "showMessage: " + mes);

		Runnable r = new Runnable() {
			@Override
			public void run() {
				final AlertDialog dialog = 
						new AlertDialog.Builder(self).setMessage(mes)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).create();
				dialog.show();				
			}
		};
		
		r.run();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// TODO what if this is different result
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			try {
				String todo = scanResult.getContents();

				FriendMessage fm = (FriendMessage)SerializableToolkit.fromString(todo);
				/*if(dbh.getFriendsTable().addFriend(fm)) {
					showMessage(fm.getName() + " added as a friend.");
				} else {
					showMessage(fm.getName() + " is already a friend.");
				}*/
			} catch (Exception e) {
				
				e.printStackTrace();
			}
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
		HugglerDatabase.closeAll();
	}
}
