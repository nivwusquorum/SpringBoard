package uk.ac.cam.cl.ss958.springboard;

import uk.ac.cam.cl.ss958.springboard.content.DatabaseContentProvider;
import uk.ac.cam.cl.ss958.springboard.content.SpringboardSqlSchema;
import uk.ac.cam.cl.ss958.springboard.content.SqlChatMessagesTable;
import uk.ac.cam.cl.ss958.springboard_huggler.SpringBoardHugglerService;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import static junit.framework.Assert.*;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SplashScreenActivity extends SherlockFragmentActivity {
	private Handler mHandler;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedStateInstance) {
		super.onCreate(savedStateInstance);
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        	getWindow().getDecorView()
        	.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        
		setContentView(R.layout.splash);
		
		
		mHandler = new Handler();
		final SherlockFragmentActivity self = this;
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent service_intent = new Intent(self, SpringBoardHugglerService.class);
				startService(service_intent);
				if (isProfileCreated()) {
					startActivity(new Intent(self, NewMainActivity.class));
				} else {
					startActivity(new Intent(self, CreateProfileActivity.class));
				}
			}
		}, 500);
	}
	
    private static Uri propertiesTableUri = 
    		Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
    				"/" + SpringboardSqlSchema.Strings.Properties.NAME);
	
	private boolean isProfileCreated() {
		 final ContentResolver cr = getContentResolver();
		 String [] projection = { SpringboardSqlSchema.Strings.Properties.KEY_VALUE };
		 String selection = SpringboardSqlSchema.Strings.Properties.KEY_NAME +" =  ?";
		 String [] selectionArgs = { SpringboardSqlSchema.Strings.Properties.P_PROFILE_CREATED };
		 
		 Cursor c = cr.query(propertiesTableUri, projection, selection , selectionArgs, null);
         c.moveToFirst();  
         assertTrue(!c.isAfterLast());
         boolean result = extractBooleanValue(c.getString(0));
         c.moveToNext();
         assertTrue(c.isAfterLast());
         return result;
	}
	
	private boolean extractBooleanValue(String b) {
		if (b.equals("true")) {
			return true;
		} else {
			assertTrue(b.equals("false"));
			return false;
		}
	}

}
