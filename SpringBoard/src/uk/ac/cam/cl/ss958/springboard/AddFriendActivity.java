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
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.widget.Toast;

public class AddFriendActivity extends SherlockFragmentActivity {
    private static final String TAG = "SpringBoard";

	private static final int REQUEST_ENABLE_BT_FOR_SCAN = 2;
	private static final int REQUEST_ENABLE_BT = 2;

	
	private Button mBtnDiscoverable;
	private Button mBtnScan;
	private ImageView mImgDiscoverable;
	private TextView mTxtDiscoverable;
	private DiscoveredFriendsFragment mFragmentBluetoothList;
	private BluetoothFriendingService mBluetooth;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addfriend);
		
		mBluetooth = new BluetoothFriendingService(this, mHandler);

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
				
				if (!mBluetooth.getAdapter().isEnabled()) {
					Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_FOR_SCAN);
				} else {
					attemptToStartScan();
				}
					
			}
		});
		
		
		onDiscoverabilityChanged(isDiscoverable());
		
		mFragmentBluetoothList = new DiscoveredFriendsFragment();
		getSupportFragmentManager().beginTransaction().
		add(R.id.bluetoothlist, mFragmentBluetoothList).commit();
		
	}


	
	@Override
	protected void onStart() {
		if (!mBluetooth.getAdapter().isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		super.onStart();
		registerReceivers();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if	(mBluetooth != null && mBluetooth.getState() == mBluetooth.STATE_NONE) {
			mBluetooth.start();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (mBluetooth != null) {
			mBluetooth.stop();
		}
	}
	
	protected void onStop() {
		super.onStop();
		unregisterReceiver(bluetoothReceiver);
	}
	
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mBluetooth != null) mBluetooth.stop();
    }


	
	void registerReceivers() {
		IntentFilter filter = 
				new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(bluetoothReceiver, filter);
		
	}
	// it requests bluetooth automatically
	void requestDiscoverability() {
		Intent discoverableIntent =
				new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivity(discoverableIntent);
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
	
	
	
	void attemptToStartScan() {
		mBluetooth.getAdapter().cancelDiscovery();
		if (!mBluetooth.getAdapter().startDiscovery()) {
			showMessage("Problem performing scan for your friends. This should not happen!", null);
		} else {
			mFragmentBluetoothList.clearAll();
			Toast.makeText(this, "Starting look up (may take a while).", Toast.LENGTH_SHORT).show();
		}
	}

	private boolean isDiscoverable() {
		return mBluetooth.getAdapter().getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
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
							"Listed as " + mBluetooth.getAdapter().getName() + ". Ask your " +
							"friend to click \"Look up visible friends\". Visibility " +
					" runs out after 2 minutes to save your battery.");
			mTxtDiscoverable.setTextColor(0xff32CD32);
		}
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
				attemptToStartScan();
			}
			if (resultCode == RESULT_CANCELED) {    
				showMessage("You must enable bluetooth to scan for visible friends!", null);
			}
		}
		
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				if (mBluetooth.getState() == mBluetooth.STATE_NONE) {
					mBluetooth.start();
				}
			}
		}
	}
	
	private void sendBluetoothMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetooth.getState() != BluetoothFriendingService.STATE_CONNECTED) {
            Log.e(TAG, "Sending message before connected!");
            friendNotAdded();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBluetooth.write(send);
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
			     mFragmentBluetoothList.addBluetoothDevice(device);
			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
											   BluetoothAdapter.STATE_OFF);
				if (state == BluetoothAdapter.STATE_ON) {
					if (mBluetooth.getState() == mBluetooth.STATE_NONE) {
						mBluetooth.start();
					}
				} else {
				    if (mBluetooth.getState() != mBluetooth.STATE_NONE) {
				    	mBluetooth.stop();
				    }
				}
			}
		}
	};
	
	public BluetoothFriendingService getBluetoothService() {
		return mBluetooth;
	}
	
	
	public static class DiscoveredFriendsFragment extends SherlockListFragment {
		
		private ArrayAdapter<String> adapter;
		private Map<Integer, BluetoothDevice> position2bluetooth;
		private BluetoothFriendingService mBluetooth;
		private AddFriendActivity parent;
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			adapter = new ArrayAdapter<String>(getActivity(),
					R.layout.simple_list_item_checkable_1,
					android.R.id.text1);
			// Populate list with our static array of titles.
			setListAdapter(adapter);
			position2bluetooth = new HashMap<Integer, BluetoothDevice>();
		}
		
		
		
		@Override
		public void onAttach(Activity activity) {
			parent = (AddFriendActivity)activity;
			mBluetooth = parent.getBluetoothService();
			super.onAttach(activity);
		}

		// returns unique name under which item was added.
		private String addItem(String item) {
			int count = 0;
			String suggested_name = "" + item;


			while(adapter.getPosition(suggested_name) != -1) {
				++count;
				suggested_name = item + " (" + count +")";
			}

			adapter.add(suggested_name);
			return suggested_name;
		}
		
		public void addBluetoothDevice(BluetoothDevice bd) {
			String name = addItem(bd.getName());
			position2bluetooth.put(adapter.getPosition(name), bd);
		}
		
		public void clearAll() {
			adapter.clear();
		}

		public void onResume() {
			super.onResume();
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			BluetoothDevice bd = position2bluetooth.get(position);
			Toast.makeText(getActivity(), "Connecting to " + bd.getAddress(), Toast.LENGTH_SHORT).show();
			mBluetooth.connect(bd);
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
	
	
	
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_CATASTROPHY = 6;
    
    public static final String TEXT = "text";
    public static final String DEVICE_NAME = "device_name";

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothFriendingService.STATE_CONNECTED:
                    //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    //mConversationArrayAdapter.clear();
                	sendBluetoothMessage("yo!");
                    break;
                case BluetoothFriendingService.STATE_CONNECTING:
                    //setStatus(R.string.title_connecting);
                    break;
                case BluetoothFriendingService.STATE_LISTEN:
                case BluetoothFriendingService.STATE_NONE:
                    //setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                Log.d(TAG, "Message sent: "+ writeMessage);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, "Message received: " + readMessage);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                //mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                //Toast.makeText(getApplicationContext(), "Connected to "
                //               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TEXT),
                               Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_CATASTROPHY:
            	showMessage(msg.getData().getString(TEXT), null);
            	friendNotAdded();
            }
        }
    };
	
	
}
