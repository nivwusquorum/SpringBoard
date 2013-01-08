package uk.ac.cam.cl.ss958.springboard;

import uk.ac.cam.cl.ss958.huggler.Huggler;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.DebugProperty;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.Property;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
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
		Intent service_intent = new Intent(this, Huggler.class);
		startService(service_intent);	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		TextView t = (TextView)findViewById(R.id.textView1);
		String boots = dbh.readDebugProperty(DebugProperty.DEVICE_BOOTS);
		String looks = dbh.readDebugProperty(DebugProperty.LOOKS_NUMBER);
		String name = dbh.readProperty(Property.HUGGLER_ID);
		t.setText("What's up, " + name + "?\nNumber Of Device boots since app install: " + boots 
				  + "\nNumber of look around actions: " + looks);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
