package uk.ac.cam.cl.ss958.springboard;

import java.lang.reflect.Field;
import java.security.KeyPair;

import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase;
import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.Property;
import uk.ac.cam.cl.ss958.springboard.MainActivity.ViewToLoad;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class QrCodeViewLoader extends ViewLoader {
	public QrCodeViewLoader(MainActivity activity) {
		super(activity);
	}

	private static final String TAG = "Springboard";

	Button backToMain;
	
	@Override
	protected void onCreate() {
		activity.setContentView(R.layout.qrcode_display);
		backToMain = (Button)activity.findViewById(R.id.backToMain);

		ImageView qrcodeImage = (ImageView)activity.findViewById(R.id.qrcodeImage);
		
		backToMain.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.loadView(ViewToLoad.MAIN_VIEW);
			}
		});
		
		try {
			HugglerDatabase hdb = HugglerDatabase.get();
			String username = hdb.readProperty(Property.HUGGLER_ID);
			KeyPair kp = hdb.getMyKeyPair();
			
			Display display = activity.getWindowManager().getDefaultDisplay();
			// TODO make sure it works on new devices. (4.0)
			int width = display.getWidth();
			int height = display.getHeight()*8/10; // * 80%

			qrcodeImage.setImageBitmap(QRGenerator.generate(username, kp, width, height));
		} catch(Exception e) {
			Log.e(TAG, "Unable to generate barcode");
		}		
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		
	}

}
