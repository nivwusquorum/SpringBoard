package uk.ac.cam.cl.ss958.springboard;

import java.util.List;

import uk.ac.cam.cl.ss958.huggler.ChatMessage;
import uk.ac.cam.cl.ss958.huggler.Huggler;
import uk.ac.cam.cl.ss958.huggler.HugglerConfig;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.DebugProperty;
import uk.ac.cam.cl.ss958.huggler.HugglerDatabase.Property;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class MainActivity extends Activity {
	static final String TAG = "SpringBoard";
	
	HugglerDatabase dbh;
	
	Handler chatHandler;
	Runnable refreshChat;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent service_intent = new Intent(this, Huggler.class);
		startService(service_intent);	
		setContentView(R.layout.activity_main);
		dbh = new HugglerDatabase(this);
		final TextView messageText = (TextView)findViewById(R.id.messageText);


		Button button = (Button)findViewById(R.id.sendButton);
		// Register the onClick listener with the implementation above
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String user = dbh.readProperty(Property.HUGGLER_ID);
				String message = messageText.getText().toString();
				dbh.getMessageTable().addMessage(new ChatMessage(user,message));
				messageText.setText("");
			}
		});
		
		final TextView messagesView = (TextView)findViewById(R.id.displayMessages);


		chatHandler = new Handler();
		refreshChat = new Runnable() {
			@Override
			public void run() {
				String name = dbh.readProperty(Property.HUGGLER_ID);
				String toDisplay = "What's up, " + name + "?\n";
				List<ChatMessage> chatMessages = 
						dbh.getMessageTable().get(10);
				for(ChatMessage m : chatMessages) {
					toDisplay = toDisplay +
							"==> " + m.toString() + "\n";
				}
				messagesView.setText(toDisplay);
				chatHandler.postDelayed(this, 1000);
			}
		};

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshChat.run();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		chatHandler.removeCallbacks(refreshChat);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
