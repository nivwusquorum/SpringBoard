package uk.ac.cam.cl.ss958.huggler.bluetooth_extension;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import uk.ac.cam.cl.ss958.huggler.HugglerExtension;

public class BluetoothExtension extends HugglerExtension {

	private static final String TAG = "Huggler";
	
	private boolean supported;
	
	private Context context;
	private BluetoothAdapter mBluetoothAdapter;
	
	public BluetoothExtension(Context context) {
		this.context = context;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();	
		if (mBluetoothAdapter == null) {
		    Log.d(TAG, "Device does not support bluetooth");
		    supported = false;
		} else {
			supported = true;
		}
	}
	
	@Override
	public void create() {
		if(!supported) return;
 
		if(!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    context.startActivity(enableBtIntent);
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() {
		if(!supported) return;

		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		if(!supported) return;

		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		if(!supported) return;

		// TODO Auto-generated method stub
		
	}

}
