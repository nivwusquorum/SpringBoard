package uk.ac.cam.cl.ss958.springboard_huggler;

import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.DebugProperty;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.AndroidCharacter;
import android.util.Log;
import android.widget.Toast;

public class IntentReceiver extends BroadcastReceiver {

    private static final String TAG = "Huggler";

    private static final String ALARM_INTENT_ACTION = "uk.ac.cam.cl.ss958.huggler.LOOK_AROUND";
    
	@Override
    public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "REVEIVED INTENT: " + intent.getAction());
    	if ( intent.getAction().equals(android.content.Intent.ACTION_BOOT_COMPLETED)) {
    		Intent service_intent = new Intent(context, SpringBoardHugglerService.class);
    		Log.d(TAG, "Attempting to start service!");
    		context.startService(service_intent);
    	}
    }

}
