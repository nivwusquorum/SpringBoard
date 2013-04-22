package uk.ac.cam.cl.ss958.springboard;

import android.app.Activity;
import android.content.Intent;

public class AddFriendActivity extends Activity {

	@Override
	protected void onResume() {
		super.onResume();
		friendAdded("Hynek");
	}
	
	void friendAdded(String name) {
		Intent returnIntent = new Intent();
		returnIntent.putExtra("friendName", name);
		setResult(RESULT_OK,returnIntent);
		finish();
	}
	
	void friendNotAdded() {
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent);        
		finish();
	}
}
