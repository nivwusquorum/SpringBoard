package uk.ac.cam.cl.ss958.springboard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.ss958.springboard.content.DatabaseContentProvider;
import uk.ac.cam.cl.ss958.springboard.content.SpringboardSqlSchema;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.zxing.common.StringUtils;


@SuppressLint("NewApi")
public class CreateProfileActivity extends SherlockActivity {
	
	TextView name;
	TextView organization;
    ImageButton profilePicture;
    Button create;
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.create_profile);

		profilePicture = (ImageButton) findViewById(R.id.profileImage);
		profilePicture.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				openImageIntent();
			}
		});
		
		profilePicture.setMaxWidth(200);
		profilePicture.setMaxHeight(200);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			profilePicture.setCropToPadding(true);
		}
		
		name = (TextView)findViewById(R.id.nameText);
		organization = (TextView)findViewById(R.id.organizationText);

		create = (Button)findViewById(R.id.profileButton);
		
		final SherlockActivity self = this;

		create.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isEmpty(name.getText().toString())) {
					t("Name must not be empty");
					return;
				}
				if (isEmpty(organization.getText().toString())) {
					t("Organization must not be empty");
					return;
				}
				if (selectedPrictureUri == null) {
					t("Please select Profile picture.");
					return;
				}
				saveProfileToDatabase();
				t("Profile successfully created.");
				startActivity(new Intent(self, NewMainActivity.class));
			}
		});
		
		if (savedInstanceState != null) {
			name.setText(savedInstanceState.getString("name"));
			name.setText(savedInstanceState.getString("organization"));
			if (savedInstanceState.getString("targetImgLocation") != null) {
				selectedPrictureUri = Uri.parse(savedInstanceState.getString("targetImgLocation"));
				profilePicture.setImageURI(selectedPrictureUri);

			} 
		}
		
	}
	

	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (selectedPrictureUri != null) {
			outState.putString("targetImgLocation", selectedPrictureUri.toString());
		}
		outState.putString("name", name.getText().toString());
		outState.putString("organization", name.getText().toString());
	}
	
    private static Uri propertiesTableUri = 
    		Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
    				"/" + SpringboardSqlSchema.Strings.Properties.NAME);
	
    private static Uri friendTableUri = 
    		Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
    				"/" + SpringboardSqlSchema.Strings.Friends.NAME);
	
    private ContentValues property(String pname, String pvalue) {
    	ContentValues cv = new ContentValues();
		cv.put(SpringboardSqlSchema.Strings.Properties.KEY_NAME, pname);
		cv.put(SpringboardSqlSchema.Strings.Properties.KEY_VALUE, pvalue);
		return cv;
    }
	
	private void saveProfileToDatabase() {
		 final ContentResolver cr = getContentResolver();
		 
		ContentValues cv = property(SpringboardSqlSchema.Strings.Properties.P_PROFILE_CREATED, "true");
		String where = SpringboardSqlSchema.Strings.Properties.KEY_NAME + " = ?";
		String [] selectionArgs = {SpringboardSqlSchema.Strings.Properties.P_PROFILE_CREATED};
		cr.update(propertiesTableUri, cv, where, selectionArgs);
		
		
		cv = property(SpringboardSqlSchema.Strings.Properties.P_USERNAME, name.getText().toString());
		cr.insert(propertiesTableUri, cv);
		
		cv = property(SpringboardSqlSchema.Strings.Properties.P_INSTITUTION, organization.getText().toString());
		cr.insert(propertiesTableUri, cv);
		
		cv = property(SpringboardSqlSchema.Strings.Properties.P_USERNAME, profileImageFile);
		cr.insert(propertiesTableUri, cv);
		
		
		// Add myself
		cv = new ContentValues();
		cv.put(SpringboardSqlSchema.Strings.Friends.KEY_NAME, name.getText().toString());
		cv.put(SpringboardSqlSchema.Strings.Friends.KEY_ORGANIZATION, organization.getText().toString());
		cv.put(SpringboardSqlSchema.Strings.Friends.KEY_IMAGE, profileImageFile);
		
	}
	
	private void t(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
	
	private boolean isEmpty(String x) {
		return x == null || "".equals(x);
	}


	private final int SELECT_PICTURE_REQUEST_CODE = 4;

	public final String profileImageFile = "profileimage.jpg";

	
	private Uri selectedPrictureUri = null;
	private Uri outputFileUri;

	
	Bitmap getPreview(Uri uri) throws URISyntaxException {
	    File image = new File(new URI(uri.toString()));

	    BitmapFactory.Options bounds = new BitmapFactory.Options();
	    bounds.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(image.getPath(), bounds);
	    if ((bounds.outWidth == -1) || (bounds.outHeight == -1))
	        return null;

	    int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight
	            : bounds.outWidth;

	    BitmapFactory.Options opts = new BitmapFactory.Options();
	    opts.inSampleSize = originalSize / 200;
	    return BitmapFactory.decodeFile(image.getPath(), opts);     
	}
	
	private void openImageIntent() {

		// Determine Uri of camera image to save.

		final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "SpringBoard" + File.separator);
		root.mkdirs();
		final String fname = "profile_image.jpg";
		final File sdImageMainDirectory = new File(root, fname);
		if (sdImageMainDirectory.exists())
			sdImageMainDirectory.delete();
		outputFileUri = Uri.fromFile(sdImageMainDirectory);

		// Camera.
		final List<Intent> cameraIntents = new ArrayList<Intent>();
		final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

		final PackageManager packageManager = getPackageManager();
		final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
		for(ResolveInfo res : listCam) {
			final String packageName = res.activityInfo.packageName;
			final Intent intent = new Intent(captureIntent);
			intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
			intent.setPackage(packageName);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			cameraIntents.add(intent);
		}

		// Filesystem.
		final Intent galleryIntent = new Intent();
		galleryIntent.setType("image/*");
		galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

		// Chooser of filesystem options.
		final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

		// Add the camera options.
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

		startActivityForResult(chooserIntent, SELECT_PICTURE_REQUEST_CODE);
	}

	private Uri storeImage(Bitmap image) {
		File file = getFileStreamPath("thumbnail.png");
		if (file.exists())
			file.delete();
	    try {
	        FileOutputStream fos = new FileOutputStream(file);
	        image.compress(Bitmap.CompressFormat.PNG, 90, fos);
	        fos.close();
	    } catch (FileNotFoundException e) {
	        Log.d(TAG, "File not found: " + e.getMessage());
	    } catch (IOException e) {
	        Log.d(TAG, "Error accessing file: " + e.getMessage());
	    }  
	    return Uri.fromFile(file);
	}
	
	private static final String TAG = "SpringBoard";
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == RESULT_OK)
		{
			if(requestCode == SELECT_PICTURE_REQUEST_CODE)
			{
				
				




				final boolean isCamera;
				if(data == null)
				{
					isCamera = true;
				}
				else
				{
					final String action = data.getAction();
					if(action == null)
					{
						isCamera = false;
					}
					else
					{
						isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
					}
				}
				Uri selectedImageUri;
				if(isCamera) {
					selectedImageUri = outputFileUri;
//					Bitmap photo = (Bitmap) data.getExtras().get("data"); 
//					profilePicture.setImageBitmap(photo);
				} else {
					selectedImageUri = data == null ? null : data.getData();
					if (selectedImageUri == null) { // maybeCamera after all??
						selectedImageUri = outputFileUri;
					}
				}
				this.selectedPrictureUri = selectedImageUri;
				try {
					Bitmap thumb = getPreview(selectedImageUri);
					selectedPrictureUri = storeImage(thumb);
					
				} catch (Exception e) {
					Toast.makeText(this, "Error while creating thumbnail!", Toast.LENGTH_LONG);
					e.printStackTrace();
				}
				profilePicture.setImageURI(selectedPrictureUri);
				profilePicture.refreshDrawableState();
				Toast.makeText(this, "Selected picture: " + selectedPrictureUri, Toast.LENGTH_LONG).show();
				
			}
		}
	}

}

