package uk.ac.cam.cl.ss958.springboard;



import java.io.File;
import java.net.URI;
import java.security.KeyPair;
import java.util.HashMap;

import uk.ac.cam.cl.ss958.huggler.databases.HugglerDatabase.Property;
import uk.ac.cam.cl.ss958.springboard.content.ChatMessage;
import uk.ac.cam.cl.ss958.springboard.content.DatabaseContentProvider;
import uk.ac.cam.cl.ss958.springboard.content.EncodedChatMessage;
import uk.ac.cam.cl.ss958.springboard.content.SpringboardSqlSchema;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.database.DatabaseUtilsCompat;
import android.support.v4.widget.SearchViewCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SearchViewCompat.OnQueryTextListenerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import static junit.framework.Assert.*;

/**
 * Demonstration of bottom to top implementation of a content provider holding
 * structured data through displaying it in the UI, using throttling to reduce
 * the number of queries done when its data changes.
 */
public class FragmentFeed extends SherlockFragmentActivity {
	// Debugging.
	static final String TAG = "SpringBoard";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FragmentManager fm = getSupportFragmentManager();

		// Create the list fragment and add it as our sole content.
		if (fm.findFragmentById(android.R.id.tabhost) == null) {
			ThrottledLoaderListFragment list = new ThrottledLoaderListFragment();
			fm.beginTransaction().add(android.R.id.tabhost, list).commit();
		}
	}

	public static class ThrottledLoaderListFragment extends HackedSherlockListFragment
	implements LoaderManager.LoaderCallbacks<Cursor> {

		// Menu identifiers
		static final int POPULATE_ID = Menu.FIRST;
		static final int CLEAR_ID = Menu.FIRST+1;

		// This is the Adapter being used to display the list's data.
		SimpleCursorAdapter mAdapter;

		SpringboardSqlSchema schema = new SpringboardSqlSchema();

		// If non-null, this is the current filter the user has provided.
		String mCurFilter;

		// Task we have running to populate the database.
		AsyncTask<Void, Void, Void> mPopulatingTask;

		private View root; 

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View listView = super.onCreateView(inflater, container, savedInstanceState);

			root = inflater.inflate(R.layout.feed, container, false);
			FrameLayout listContainer = (FrameLayout)root.findViewById(R.id.list_view);
			listContainer.addView(listView);

			return root;
		}

		@Override public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			setEmptyText("No messages received.");
			setHasOptionsMenu(true);

			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new SimpleCursorAdapter(getActivity(),
					R.layout.feedlistitem, null,
					new String[] { SpringboardSqlSchema.Strings.Messages.KEY_USER,
								   SpringboardSqlSchema.Strings.Messages.KEY_MESSAGE },
					new int[] { R.id.user, R.id.message }, 0) {
				@Override
				public void bindView(View view, Context context,
						Cursor c) {
					super.bindView(view, context, c);
					ImageView img = (ImageView)view.findViewById(R.id.profileIcon);
					img.setMaxWidth(50);

					String username = null;
					for (int i=0; i< c.getColumnCount(); ++i) {
						if (c.getColumnName(i).equals(SpringboardSqlSchema.Strings.Messages.KEY_USER)) {
							username = c.getString(i);
						}
						
					}
					
					Log.d(TAG, "looking up avatar for " + username);
					
					final ContentResolver cr = getActivity().getContentResolver();
					
					String [] projection = { SpringboardSqlSchema.Strings.Friends.KEY_IMAGE };
					
					String selection = SpringboardSqlSchema.Strings.Friends.KEY_NAME +" =  ?";
					String [] selectionArgs = { username };
					
					Cursor cfriend = cr.query(friendsTableUri, projection, selection, selectionArgs, null);
					cfriend.moveToFirst();
					if (cfriend.isAfterLast()) {
						Log.d(TAG, "No avatar");
						img.setImageResource(R.drawable.choose_profile);
					} else {
						Log.d(TAG, "Maybe uri: " + cfriend.getString(0));
						Uri uri = Uri.parse(cfriend.getString(0));
						img.setImageURI(uri);
					}
					
				}
				
				

			};
			setListAdapter(mAdapter);

			// Start out with a progress indicator.
			setListShown(false);

			// Prepare the loader.  Either re-connect with an existing one,
			// or start a new one.
			getLoaderManager().initLoader(0, null, this);

			final TextView messageText = (TextView)root.findViewById(R.id.messageText);

			Button sendButton = (Button)root.findViewById(R.id.sendButton);

			sendButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final ContentResolver cr = getActivity().getContentResolver();


					// GET PROPERTIES NEEDED.
					String username = null;
					Integer nextmsgid = null;

					String [] projection = { SpringboardSqlSchema.Strings.Properties.KEY_NAME,
							SpringboardSqlSchema.Strings.Properties.KEY_VALUE };

					Cursor c = cr.query(propertiesTableUri, projection, null , null, null);
					c.moveToFirst();  
					while (!c.isAfterLast()) {
						if (SpringboardSqlSchema.Strings.Properties.P_USERNAME.equals(c.getString(0))) {
							username = c.getString(1);
						} else if (SpringboardSqlSchema.Strings.Properties.P_NEXTMSGID.equals(c.getString(0))) {
							nextmsgid = Integer.valueOf(c.getString(1));
						}
						c.moveToNext();
					}

					// DELETE PROPERTIES TO BE UPDATED
					String where = SpringboardSqlSchema.Strings.Properties.KEY_NAME + "=?";
					String [] selectionArgs = {SpringboardSqlSchema.Strings.Properties.P_NEXTMSGID};
					int r = cr.delete(propertiesTableUri, where, selectionArgs);
					assertTrue(r == 1);

					ContentValues cv = property(SpringboardSqlSchema.Strings.Properties.P_NEXTMSGID, "" + (nextmsgid+1));
					cr.insert(propertiesTableUri, cv);

					ContentValues values = new ContentValues();
					values.put(SpringboardSqlSchema.Strings.Messages.KEY_MSGID, nextmsgid);
					// TODO: allow to chose target
					values.put(SpringboardSqlSchema.Strings.Messages.KEY_TARGET,
							SpringboardSqlSchema.Strings.Messages.TARGET_EVERYONE);

					values.put(SpringboardSqlSchema.Strings.Messages.KEY_TIMESTAMP,"" +System.currentTimeMillis());

					values.put(SpringboardSqlSchema.Strings.Messages.KEY_USER, username);

					values.put(SpringboardSqlSchema.Strings.Messages.KEY_MESSAGE, messageText.getText().toString());

					cr.insert(messageTableUri, values);
					
					messageText.setText("");
					messageText.clearFocus();
				}
			});
		}

		private static Uri propertiesTableUri = 
				Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
						"/" + SpringboardSqlSchema.Strings.Properties.NAME);

	    private ContentValues property(String pname, String pvalue) {
	    	ContentValues cv = new ContentValues();
			cv.put(SpringboardSqlSchema.Strings.Properties.KEY_NAME, pname);
			cv.put(SpringboardSqlSchema.Strings.Properties.KEY_VALUE, pvalue);
			return cv;
	    }
		
		@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			MenuItem item = menu.add("Search");
			item.setIcon(android.R.drawable.ic_menu_search);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			View searchView = SearchViewCompat.newSearchView(getActivity());
			if (searchView != null) {
				SearchViewCompat.setOnQueryTextListener(searchView,
						new OnQueryTextListenerCompat() {
					@Override
					public boolean onQueryTextChange(String newText) {
						// Called when the action bar search text has changed.  Update
						// the search filter, and restart the loader to do a new query
						// with this filter.
						mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
						getLoaderManager().restartLoader(0, null, ThrottledLoaderListFragment.this);
						return true;
					}
				});
				item.setActionView(searchView);
			}

			MenuItem clearItem = menu.add(Menu.NONE, CLEAR_ID, 0, "Clear");
			clearItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}

		private static Uri messageTableUri = 
				Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
						"/" + SpringboardSqlSchema.Strings.Messages.NAME);

		@Override public boolean onOptionsItemSelected(MenuItem item) {
			final ContentResolver cr = getActivity().getContentResolver();

			switch (item.getItemId()) {
			case CLEAR_ID:
				if (mPopulatingTask != null) {
					mPopulatingTask.cancel(false);
					mPopulatingTask = null;
				}
				AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
					@Override protected Void doInBackground(Void... params) {
						cr.delete(messageTableUri, null, null);
						return null;
					}
				};
				task.execute((Void[])null);
				return true;

			default:
				return super.onOptionsItemSelected(item);
			}
		}

		@Override public void onListItemClick(ListView l, View v, int position, long id) {
			// Insert desired behavior here.
			Log.i(TAG, "Item clicked: " + id);
		}

		// These are the rows that we will retrieve.
		static final String[] PROJECTION = new String[] {
			SpringboardSqlSchema.Strings.Messages.KEY_ID,
			SpringboardSqlSchema.Strings.Messages.KEY_MESSAGE,
			SpringboardSqlSchema.Strings.Messages.KEY_USER,

		};

		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			final ContentResolver cr = getActivity().getContentResolver();

			String username = null;

			String [] projection = { SpringboardSqlSchema.Strings.Properties.KEY_NAME,
					SpringboardSqlSchema.Strings.Properties.KEY_VALUE };

			Cursor c = cr.query(propertiesTableUri, projection, null , null, null);
			c.moveToFirst();  
			while (!c.isAfterLast()) {
				if (SpringboardSqlSchema.Strings.Properties.P_USERNAME.equals(c.getString(0))) {
					username = c.getString(1);
				}
				c.moveToNext();
			}
			
			
			
			
			
			String selection = SpringboardSqlSchema.Strings.Messages.KEY_TARGET + " IN (?,?)";
			String [] selectionArgs = { SpringboardSqlSchema.Strings.Messages.TARGET_EVERYONE,
										username };
			
			if (!TextUtils.isEmpty(mCurFilter)) {
				selection = selection + " AND " +  SpringboardSqlSchema.Strings.Messages.KEY_MESSAGE +
						" LIKE ?";
				selectionArgs = new String[] { selectionArgs[0], selectionArgs[1], "%" + mCurFilter + "%" };
			}

			CursorLoader cl = new CursorLoader(getActivity(), messageTableUri,
					PROJECTION, selection, selectionArgs, SpringboardSqlSchema.Strings.Messages.KEY_TIMESTAMP + " DESC");
			cl.setUpdateThrottle(2000); // update at most every 2 seconds.
			return cl;
		}

	    private static Uri friendsTableUri = 
	    		Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
	    				"/" + SpringboardSqlSchema.Strings.Friends.NAME);
		
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mAdapter.swapCursor(data);

			// The list should now be shown.
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
		}

		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.swapCursor(null);
		}





	}
}

