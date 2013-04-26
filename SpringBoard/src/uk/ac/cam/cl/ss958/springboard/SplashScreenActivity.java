package uk.ac.cam.cl.ss958.springboard;

import uk.ac.cam.cl.ss958.springboard_huggler.SpringBoardHugglerService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

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
				startActivity(new Intent(self, NewMainActivity.class));
			}
		}, 200);
		
		
	}

}
