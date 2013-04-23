package uk.ac.cam.cl.ss958.springboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AddFriendActivity extends Activity {

	private BluetoothAdapter mBluetoothAdapter;
	private static final int REQUEST_ENABLE_BT = 2;

	private Button mBtnDiscoverable;
	private Button mBtnScan;
	private ImageView mImgDiscoverable;
	private TextView mTxtDiscoverable;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addfriend);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter == null) {

			showMessage("Currently the only way to get friends is Bluetooth. " +
					"Your decvice does not support it. " +
					"Apologies for the inconvenience :(",
					new Runnable() {
				@Override
				public void run() {
					friendNotAdded();
				}
			});
			return;
		}

		mBtnDiscoverable = (Button)findViewById(R.id.btnbluetoothdiscoverable);
		mBtnScan = (Button)findViewById(R.id.btnbluetoothscan);
		mImgDiscoverable = (ImageView)findViewById(R.id.imgbluetoothdiscoverable);
		mTxtDiscoverable = (TextView)findViewById(R.id.txtbluetoothdiscoverable);

		mBtnDiscoverable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!isDiscoverable()) 
					requestDiscoverability();
				onDiscoverabilityChanged(isDiscoverable());
			}
		});
		IntentFilter filter = 
				new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		registerReceiver(bluetoothReceiver, filter);
		onDiscoverabilityChanged(isDiscoverable());


	}

	private boolean isDiscoverable() {
		return mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
	}

	void onDiscoverabilityChanged(boolean discoverable) {
		if (!discoverable) {
			mImgDiscoverable.setImageResource(R.drawable.discovery_off);
			mTxtDiscoverable.setText(
					"You are currently not visible to nearby people " +
							"who would like to add you as a friend. Click button " +
					"below to make yourself visible");
			mTxtDiscoverable.setTextColor(0xffff6666);

		} else {
			mImgDiscoverable.setImageResource(R.drawable.discovery_on);
			mTxtDiscoverable.setText(
					"You are discoverable for your friends now. Ask your " +
							"friend to click \"Look up visible friends\" for him " +
							"to be able to add you. Visibility runs out after " +
					"2 minutes to save your battery");
			mTxtDiscoverable.setTextColor(0xff80ff00);
		}
	}

	//This will handle the broadcast
	private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
		//@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			// nope cannot do switch on strings - already tried!
			if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
				int scanMode = intent.getIntExtra(
						BluetoothAdapter.EXTRA_SCAN_MODE,
						BluetoothAdapter.SCAN_MODE_NONE); // defualt
				boolean isDiscoverable = 
						scanMode == 
						BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
				onDiscoverabilityChanged(isDiscoverable);
			}
		}
	};





	@Override
	protected void onStart() {
		super.onStart();

	}

	void requestBluetooth() {
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	// it requests bluetooth automatically
	void requestDiscoverability() {
		Intent discoverableIntent =
				new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivity(discoverableIntent);
	}



	@Override
	protected void onResume() {
		super.onResume();
		//friendAdded("Hynek");
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


	Handler messageHandler;

	public void showMessage(String message, final Runnable callback) {
		final String mes = message;
		if(messageHandler == null) { 
			messageHandler = new Handler();
		}
		final Activity self = this;

		Runnable r = new Runnable() {
			@Override
			public void run() {
				final AlertDialog dialog = 
						new AlertDialog.Builder(self).setMessage(mes)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								callback.run();
							}
						}).create();
				dialog.show();				
			}
		};

		r.run();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_ENABLE_BT) {
			if(resultCode == RESULT_OK){      

			}
			if (resultCode == RESULT_CANCELED) {    
				friendNotAdded();
			}
		}
	}
}
