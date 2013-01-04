package uk.ac.cam.cl.ss958.springboard;

import uk.ac.cam.cl.ss958.huggler.HugglerDatabase;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {
	static final String TAG = "SpringBoard";
	
	HugglerDatabase dbh;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		dbh = new HugglerDatabase(this);
		SSDPtest1();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		TextView t = (TextView)findViewById(R.id.textView1);
		int boots = dbh.readDebugProperty(HugglerDatabase.DPROPERTY_DEVICE_BOOTS);
		int looks = dbh.readDebugProperty(HugglerDatabase.DPROPERTY_LOOKS_NUMBER);
		t.setText("What's up?\nNumber Of Device boots since app install: " + boots 
				  + "\nNumber of look around actions: " + looks);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	private void SSDPtest1() {
    	new Thread(new Runnable() {
    	    public void run() {
    	        Log.e(TAG, "Hello from a thread!");        
    	    }
    	}).start();

	}

}
