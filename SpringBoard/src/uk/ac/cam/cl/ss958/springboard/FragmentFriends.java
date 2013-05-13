package uk.ac.cam.cl.ss958.springboard;


import static junit.framework.Assert.assertTrue;
import uk.ac.cam.cl.ss958.springboard.FragmentFeed.ThrottledLoaderListFragment;
import uk.ac.cam.cl.ss958.springboard.content.DatabaseContentProvider;
import uk.ac.cam.cl.ss958.springboard.content.SpringboardSqlSchema;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SearchViewCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SearchViewCompat.OnQueryTextListenerCompat;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.util.Log;

/**
 * Demonstration of using fragments to implement different activity layouts.
 * This sample provides a different layout (and activity flow) when run in
 * landscape.
 */
public class FragmentFriends extends SherlockFragmentActivity {


	public static final String TAG = "SpringBoard";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_layout_support);
	}


	/**
	 * This is a secondary activity, to show what the user has selected
	 * when the screen is not large enough to show it all in one activity.
	 */

	public static class DetailsActivity extends SherlockFragmentActivity {

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			if (getResources().getConfiguration().orientation
					== Configuration.ORIENTATION_LANDSCAPE) {
				// If the screen is now in landscape mode, we can show the
				// dialog in-line with the list so we don't need this activity.
				finish();
				return;
			}

			if (savedInstanceState == null) {
				// During initial setup, plug in the details fragment.
				DetailsFragment details = new DetailsFragment();
				details.setArguments(getIntent().getExtras());
				getSupportFragmentManager().beginTransaction().add(
						android.R.id.content, details).commit();
			}
		}
	}

	public static final String FRIENDLISTTAG = "friendlist";
	public static final String DETAILSTAG = "details";

	/**
	 * This is the "top-level" fragment, showing a list of items that the
	 * user can pick.  Upon picking an item, it takes care of displaying the
	 * data to the user as appropriate based on the currrent UI layout.
	 */

	public static class ContainerFragment extends SherlockFragment { 
		private int mCurIndex = 0;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			if (savedInstanceState != null) {
				// Restore last state for checked position.
				mCurIndex = savedInstanceState.getInt("mCurIndex", 0);
			}
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			// super.onCreateView(inflater, container, savedInstanceState);

			View root = inflater.inflate(R.layout.fragment_layout_support, container, false);

			return root;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setHasOptionsMenu(true);
		}

		@Override
		public void onResume() {
			super.onStart();

			// Supply index input as an argument.
			TitlesFragment titles = new TitlesFragment();
			Bundle args = new Bundle();
			args.putInt("index", mCurIndex);
			titles.setArguments(args);


			getFragmentManager().beginTransaction().add(
					R.id.layout_support_content,  
					titles, FRIENDLISTTAG).commit();

		}

		@Override
		public void onPause() {
			super.onStop();

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			Fragment friendlist = getFragmentManager().findFragmentByTag(FRIENDLISTTAG);
			if (friendlist != null) 
				ft.remove(friendlist);
			Fragment details = getFragmentManager().findFragmentByTag(DETAILSTAG);
			if (details != null) {
				DetailsFragment df = (DetailsFragment)details;
				mCurIndex = df.getShownIndex();
				ft.remove(details);
			}
			ft.commit();
		}

		static final int  OPTION_ADD_FRIEND = Menu.FIRST;

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			MenuItem friendItem = menu.add(Menu.NONE, OPTION_ADD_FRIEND, 0, "Add Friend");
			friendItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {

			switch (item.getItemId()) {
			case OPTION_ADD_FRIEND:
				Intent i = new Intent(getActivity(),
						AddFriendActivity.class);
				startActivityForResult(i, 1);

			default:
				return super.onOptionsItemSelected(item);
			}
		}

		public void onActivityResult(int requestCode, int resultCode, Intent data) {

			if (requestCode == 1) {
				if(resultCode == RESULT_OK){      
					String friendName = data.getStringExtra("friendName");
					Toast.makeText(getActivity(), 
							"Added " + friendName + " as a friend.",
							Toast.LENGTH_SHORT).show();
				}
				if (resultCode == RESULT_CANCELED) {    
					Toast.makeText(getActivity(),
							"Error adding friend!",
							Toast.LENGTH_SHORT).show();
				}
			}
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putInt("mCurIndex", mCurIndex);
		}

	}


	public static class TitlesFragment extends SherlockListFragment  implements LoaderManager.LoaderCallbacks<Cursor>  {
		boolean mDualPane;
		int mCurCheckPosition = 0;

		private SimpleCursorAdapter mAdapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			setEmptyText("No friends added.");

			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new SimpleCursorAdapter(getActivity(),
					android.R.layout.simple_list_item_1, null,
					new String[] { SpringboardSqlSchema.Strings.Friends.KEY_NAME },
					new int[] { android.R.id.text1 }, 0);
			setListAdapter(mAdapter);

			if (getArguments() != null) {
				mCurCheckPosition =  getArguments().getInt("index", 0);
			} else if (savedInstanceState != null) {
				// Restore last state for checked position.
				mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
			}
			calculateListProperties();

			setListShown(false);

			getLoaderManager().initLoader(0, null, this);

			if (mDualPane) {
				showDetails(mCurCheckPosition);
			}

		}

		private void calculateListProperties() {
			// Check to see if we have a frame in which to embed the details
			// fragment directly in the containing UI.
			View detailsFrame = getActivity().findViewById(R.id.details);
			mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

			if (mDualPane) {
				// In dual-pane mode, the list view highlights the selected item.
				getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				// Make sure our UI is in the correct state.
			}
		}

		public void onResume() {
			super.onResume();

			calculateListProperties();
		}


		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putInt("curChoice", mCurCheckPosition);
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {

			showDetails(position);
		}

		/**
		 * Helper function to show the details of a selected item, either by
		 * displaying a fragment in-place in the current UI, or starting a
		 * whole new activity in which it is displayed.
		 */
		void showDetails(int index) {
			mCurCheckPosition = index;

			calculateListProperties();

			Cursor c = mAdapter.getCursor();
			String username = null;
			if (c != null) {
				c.moveToPosition(index);
				username = c.getString(1);
			}


			if (mDualPane) {        	   
				// We can display everything in-place with fragments, so update
				// the list to highlight the selected item and show the data.
				getListView().setItemChecked(index, true);
				// Check what fragment is currently shown, replace if needed.
				DetailsFragment details = (DetailsFragment)
						getFragmentManager().findFragmentById(R.id.details);
				if (details == null || details.getShownIndex() != index) {

					// Make new fragment to show this selection.
					details = DetailsFragment.newInstance(index, username, false);

					// Execute a transaction, replacing any existing fragment
					// with this one inside the frame.
					FragmentTransaction ft = getFragmentManager().beginTransaction();
					ft.replace(R.id.details, details);
					ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
					ft.commit();
				}

			} else {
				/*// Otherwise we need to launch a new activity to display
               // the dialog fragment with selected text.
               Intent intent = new Intent();
               intent.setClass(getActivity(), DetailsActivity.class);
               intent.putExtra("index", index);
               startActivity(intent);*/

				// Make new fragment to show this selection.
				DetailsFragment details = DetailsFragment.newInstance(index, username,  true);

				// Execute a transaction, replacing any existing fragment
				// with this one inside the frame.
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.remove(getFragmentManager().findFragmentByTag(FRIENDLISTTAG));
				ft.add(R.id.layout_support_content, details, DETAILSTAG);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft.commit();

			}
		}

		private static Uri friendsTableUri = 
				Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
						"/" + SpringboardSqlSchema.Strings.Friends.NAME);

		// These are the rows that we will retrieve.
		static final String[] PROJECTION = new String[] {
			SpringboardSqlSchema.Strings.Friends.KEY_ID,
			SpringboardSqlSchema.Strings.Friends.KEY_NAME,
		};

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {

			CursorLoader cl = new CursorLoader(getActivity(), friendsTableUri,
					PROJECTION, null, null, null);
			cl.setUpdateThrottle(2000);

			return cl;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mAdapter.swapCursor(data);
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}			
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.swapCursor(null);
		}
	}


	/**R.layout.fragment_layout_support
	 * This is the secondary fragment, displaying the details of a particular
	 * item.
	 */

	public static class DetailsFragment extends SherlockFragment {
		/**
		 * Create a new instance of DetailsFragment, initialized to
		 * show the text at 'index'.
		 */
		public static DetailsFragment newInstance(int index, String username, boolean addBack) {
			DetailsFragment f = new DetailsFragment();

			// Supply index input as an argument.
			Bundle args = new Bundle();
			args.putInt("index", index);
			args.putBoolean("addBack", addBack);
			args.putString("username", username);
			f.setArguments(args);

			return f;
		}

		public int getShownIndex() {
			if (getUsername() == null) 
				return -1;
			return getArguments().getInt("index", 0);
		}

		public String getUsername() {
			return getArguments().getString("username");
		}
		
	    private static Uri friendsTableUri = 
	    		Uri.parse("content://" + DatabaseContentProvider.AUTHORITY +
	    				"/" + SpringboardSqlSchema.Strings.Friends.NAME);

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			if (container == null) {
				// We have different layouts, and in one of them this
				// fragment's containing frame doesn't exist.  The fragment
				// may still be created fromgnm its saved state, but there is
				// no reason to try to create its view hierarchy because it
				// won't be displayed.  Note this is not needed -- we could
				// just run the code below, where we would create and return
				// the view hierarchy; it would just never be used.
				return null;
			}


			String u = getUsername();
			View top;
			if (u == null) {
				LinearLayout l = new LinearLayout(getActivity());
				TextView t;
				l.addView(t = new TextView(getActivity()));
				t.setText("Please select user");
				return l;
			} else {
				top = inflater.inflate(R.layout.profile, null);
			}

			final ContentResolver cr = getActivity().getContentResolver();
			
			String [] projection = { SpringboardSqlSchema.Strings.Friends.KEY_NAME,
									 SpringboardSqlSchema.Strings.Friends.KEY_ORGANIZATION,
									 SpringboardSqlSchema.Strings.Friends.KEY_IMAGE };
			
			String selection = SpringboardSqlSchema.Strings.Friends.KEY_NAME +" =  ?";
			String [] selectionArgs = { u };

			Cursor c = cr.query(friendsTableUri, projection, selection , selectionArgs, null);
			c.moveToFirst();  
			assertTrue(!c.isAfterLast());
			String name = c.getString(0);
			String organization = c.getString(1);
			Uri picture = Uri.parse(c.getString(2));
			c.moveToNext();
			assertTrue(c.isAfterLast());
			
			TextView nameView = (TextView)top.findViewById(R.id.profileName);
			TextView organizationView = (TextView)top.findViewById(R.id.profileOrganization);
			ImageView pictureView = (ImageView)top.findViewById(R.id.profileImg);

			nameView.setText("name: " + name);
			organizationView.setText("organization: " + organization);
			
			pictureView.setImageURI(picture);

			

			if(getArguments().getBoolean("addBack", false)) {
				Button btn = new Button(getActivity());
				btn.setText("Back to List");

	
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
															RelativeLayout.LayoutParams.MATCH_PARENT,
															RelativeLayout.LayoutParams.WRAP_CONTENT);
				lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				btn.setLayoutParams(lp);
				final SherlockFragment self = this; 
				btn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {

						FragmentTransaction ft = getFragmentManager().beginTransaction();
						ft.remove(self);
						ft.add(R.id.layout_support_content,  
								new TitlesFragment(), FRIENDLISTTAG);
						ft.commit();
					}

				});

				RelativeLayout parent = (RelativeLayout)top.findViewById(R.id.profileAdditional);
				
				parent.addView(btn);
			}
			
			return top;
			/*ScrollView scroller = new ScrollView(getActivity());
			TextView text = new TextView(getActivity());
			int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					4, getActivity().getResources().getDisplayMetrics());
			text.setPadding(padding, padding, padding, padding);
			scroller.addView(text);
			String u = getUsername();
			if (u == null) {
				text.setText("Please select user.");
			} else {
				text.setText("username: " + u);
			}
			//scroller.setFillViewport(true);

			LinearLayout fl = new LinearLayout(getActivity());
			LinearLayout.LayoutParams params =
					new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT,
							1.0f);
			fl.setLayoutParams(params);
			fl.addView(scroller);

			LinearLayout ll = new LinearLayout(getActivity());
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.addView(fl);

			if(getArguments().getBoolean("addBack", false)) {
				Button btn = new Button(getActivity());
				btn.setText("Back to List");

				btn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT));
				final SherlockFragment self = this; 
				btn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {

						FragmentTransaction ft = getFragmentManager().beginTransaction();
						ft.remove(self);
						ft.add(R.id.layout_support_content,  
								new TitlesFragment(), FRIENDLISTTAG);
						ft.commit();
					}

				});

				ll.addView(btn);
			}
			return ll;
			 */
		}
	}

}
