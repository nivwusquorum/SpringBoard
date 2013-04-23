package uk.ac.cam.cl.ss958.springboard;

import uk.ac.cam.cl.ss958.springboard.FragmentFriends.DetailsFragment;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Comparator;
import android.widget.Toast;

public class AddFriendActivity extends SherlockFragmentActivity {

	private BluetoothAdapter mBluetoothAdapter;
	private static final int REQUEST_ENABLE_BT_FOR_SCAN = 2;

	private Button mBtnDiscoverable;
	private Button mBtnScan;
	private ImageView mImgDiscoverable;
	private TextView mTxtDiscoverable;
	private DiscoveredFriendsFragment mFragmentBluetoothList;


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
		
		mBtnScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (!mBluetoothAdapter.isEnabled()) {
					Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_FOR_SCAN);
				} else {
					attemptToStartScan();
				}
					
			}
		});
		
		IntentFilter filter = 
				new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		registerReceiver(bluetoothReceiver, filter);
		
		onDiscoverabilityChanged(isDiscoverable());
		
		mFragmentBluetoothList = new DiscoveredFriendsFragment();
		getSupportFragmentManager().beginTransaction().
		add(R.id.bluetoothlist, mFragmentBluetoothList).commit();
	}
	
	void attemptToStartScan() {
		mBluetoothAdapter.cancelDiscovery();
		if (!mBluetoothAdapter.startDiscovery()) {
			showMessage("Problem performing scan for your friends. This should not happen!", null);
		} else {
			mFragmentBluetoothList.clearAll();
			Toast.makeText(this, "Starting look up (may take a while).", Toast.LENGTH_SHORT).show();
		}
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
					"You are discoverable for your friends now. You will be " +
							"Listed as " + mBluetoothAdapter.getName() + ". Ask your " +
							"friend to click \"Look up visible friends\". Visibility " +
					" runs out after 2 minutes to save your battery.");
			mTxtDiscoverable.setTextColor(0xff32CD32);
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
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
			     BluetoothDevice device = 
			    		 intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			     mFragmentBluetoothList.addItem(device.getName());
			}
		}
	};





	@Override
	protected void onStart() {
		super.onStart();

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
								if (callback != null) {
									callback.run();
								}
							}
						}).create();
				dialog.show();				
			}
		};

		r.run();
	}

	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_ENABLE_BT_FOR_SCAN) {
			if(resultCode == RESULT_OK){      
				
			}
			if (resultCode == RESULT_CANCELED) {    
				showMessage("You must enable bluetooth to scan for visible friends!", null);
			}
		}
	}
	

	
	
	
	
	public static class DiscoveredFriendsFragment extends SherlockListFragment {
		static
		private ArrayAdapter<String> adapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			adapter = new ArrayAdapter<String>(getActivity(),
					R.layout.simple_list_item_checkable_1,
					android.R.id.text1);
			// Populate list with our static array of titles.
			setListAdapter(adapter);
		}

		// returns unique name under which item was added.
		public String addItem(String item) {
			int count = 0;
			String suggested_name = "" + item;


			while(adapter.getPosition(suggested_name) != -1) {
				++count;
				suggested_name = item + " (" + count +")";
			}

			adapter.add(suggested_name);
			adapter.sort(new StringComparator());
			return suggested_name;
		}
		
		public void clearAll() {
			adapter.clear();
		}

		public void onResume() {
			super.onResume();
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {

		}

		public class StringComparator implements Comparator<String>{
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
		}


	}
}
