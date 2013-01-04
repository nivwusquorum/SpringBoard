package uk.ac.cam.cl.ss958.huggler;

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
	    	dbh.onBoot();
	    	setAlarm(context);
    	} else if (intent.getAction().equals(ALARM_INTENT_ACTION)) {
    		dbh.onLookAround();
    	}
    }
	
	private void setAlarm(Context context) {
		AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(ALARM_INTENT_ACTION);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
						System.currentTimeMillis(),
						1000 * 60 * HugglerConfig.UPDATE_INTERVAL_M,
						pi);
	}

}
