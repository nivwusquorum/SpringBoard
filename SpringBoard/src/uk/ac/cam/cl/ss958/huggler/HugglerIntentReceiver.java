package uk.ac.cam.cl.ss958.huggler;

import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.DebugProperty;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.AndroidCharacter;
import android.util.Log;
import android.widget.Toast;

public class HugglerIntentReceiver extends BroadcastReceiver {

    private static final String TAG = "SpringBoard";

    private static final String ALARM_INTENT_ACTION = "uk.ac.cam.cl.ss958.huggler.LOOK_AROUND";
    
	@Override
    public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "REVEIVED INTENT: " + intent.getAction());
    	HugglerDatabase dbh = new HugglerDatabase(context);
    	if ( intent.getAction().equals(android.content.Intent.ACTION_BOOT_COMPLETED)) {
	    	dbh.incrementDebugProperty(DebugProperty.DEVICE_BOOTS);
    		Intent service_intent = new Intent(context, Huggler.class);
    		context.startService(service_intent);
    	}
    }

}
