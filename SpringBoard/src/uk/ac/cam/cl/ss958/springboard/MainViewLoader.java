package uk.ac.cam.cl.ss958.springboard;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.List;

import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.Property;
import uk.ac.cam.cl.ss958.springboard.MainActivity.ViewToLoad;
import uk.ac.cam.cl.ss958.springboard.content.ChatMessage;
import uk.ac.cam.cl.ss958.springboard.content.EncodedChatMessage;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainViewLoader extends ViewLoader {

	public MainViewLoader(MainActivity activity) {
		super(activity);
	}
	
	Handler chatHandler;
	Runnable refreshChat;
	
	Button sendButton;
	Button getFriend;
	Button beFriend;
		
	HugglerDatabase dbh;
	
	@Override
	protected void onCreate() {

		activity.setContentView(R.layout.activity_main); 

		dbh = HugglerDatabase.get();
		
		final TextView messageText = (TextView)activity.findViewById(R.id.messageText);

		messageText.clearFocus();
		
		sendButton = (Button)activity.findViewById(R.id.sendButton);
		getFriend = (Button)activity.findViewById(R.id.getfriend);
		beFriend = (Button)activity.findViewById(R.id.befriend);
		
		// Register the onClick listener with the implementation above
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*String user = dbh.readProperty(Property.HUGGLER_ID);
				String message = messageText.getText().toString();
				KeyPair kp = dbh.getMyKeyPair();
				try {
					ChatMessage cm = new ChatMessage(user,message);
					// EncodedChatMessage.testEncryption(cm, kp);
					EncodedChatMessage ecm = 
							new EncodedChatMessage(cm,
									               kp.getPrivate());
					dbh.getMessageTable().addEncodedMessage(ecm, kp.getPublic());
					messageText.setText("");
				} catch (Exception e) {
					activity.showMessage("Unable to send message (" + e.getMessage() + ")");
				}*/
			}
		});
		
		getFriend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.initiateBarcodeScanForFriend();
			}
		});
		
		beFriend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.loadView(ViewToLoad.QRCODE_VIEW);
			}
		});
		
		final TextView messagesView = (TextView)activity.findViewById(R.id.displayMessages);


		chatHandler = new Handler();
		refreshChat = new Runnable() {
			@Override
			public void run() { 
				/*if (dbh == null) {
					Log.wtf("Huggler", "DBH is not initialized.");
				}
				String name = dbh.readProperty(Property.HUGGLER_ID);
				String toDisplay = "What's up, " + name + "?\n";
				toDisplay += "Remember that you need to add person as a friend before you can receive that person's messages!\n";
				List<ChatMessage> chatMessages = 
						dbh.getMessageTable().get(10);
				for(ChatMessage m : chatMessages) {
					toDisplay = toDisplay +
							"==> " + m.toString() + "\n";
				}
				messagesView.setText(toDisplay);
				chatHandler.postDelayed(this, 1000);*/
			}
		};
	}

	@Override
	protected void onResume() {
		refreshChat.run();
	}

	@Override
	protected void onPause() {
		chatHandler.removeCallbacks(refreshChat);
	}

	@Override
	protected void onDestroy() {

	}

}
