package uk.ac.cam.cl.ss958.springboard;

import uk.ac.cam.cl.ss958.springboard.MainActivity.ViewToLoad;
import android.util.Log;
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
			qrcodeImage.setImageBitmap(QRGenerator.generate());
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
