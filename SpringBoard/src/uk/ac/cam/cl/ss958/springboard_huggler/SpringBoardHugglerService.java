package uk.ac.cam.cl.ss958.springboard_huggler;

import java.util.concurrent.atomic.AtomicBoolean;

import uk.ac.cam.cl.ss958.huggler.Huggler;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SpringBoardHugglerService extends Service {

	AtomicBoolean running;

	Huggler huggler;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// We don't provide binding, so return null
		return null;
	}
	
	@Override
	public void onCreate() {
		Log.d("Huggler", "Service started?");
		running = new AtomicBoolean(false);
		huggler = new Huggler((Context)this);
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(running.compareAndSet(false, true)) {
			Toast.makeText(this, "Huggler service started. ", Toast.LENGTH_SHORT).show();
			huggler.start();
		}
		// If we get killed, after returning from here, restart well if unlucky
		// may still fail, better check that is running each time activity is
		// started.
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		huggler.destroy();
		if(running.compareAndSet(true, false)) {			
			Toast.makeText(this, "Huggler service stopped.", Toast.LENGTH_SHORT).show(); 
		}
	}
	

}
