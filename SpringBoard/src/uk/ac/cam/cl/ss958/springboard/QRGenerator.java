package uk.ac.cam.cl.ss958.springboard;

import java.io.IOException;
import java.security.KeyPair;

import uk.ac.cam.cl.ss958.toolkits.SerializableToolkit;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QRGenerator {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;

	public static Bitmap generate(String username,
								  KeyPair kp, 
								  int QRwidth,
								  int QRheight) throws WriterException {
		QRheight = Math.min(QRwidth, QRheight);
		QRwidth = Math.min(QRwidth, QRheight);
		
		String message = null;
		try {
			message = SerializableToolkit.toString(new FriendMessage(username, kp.getPublic()));
		} catch (IOException e) {
			Log.e("SpringBoard", "Unable to serialize public key(" +e.getMessage() +")");
		}
		
		
		MultiFormatWriter writer = new MultiFormatWriter();
		BitMatrix result;
		try {
			result = writer.encode(message, BarcodeFormat.QR_CODE, QRwidth, QRheight, null);
		} catch (IllegalArgumentException iae) {
			// Unsupported format
			return null;
		}
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		

		
		return bitmap;
	}
}
