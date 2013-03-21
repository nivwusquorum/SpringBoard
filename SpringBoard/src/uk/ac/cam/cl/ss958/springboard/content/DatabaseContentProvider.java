package uk.ac.cam.cl.ss958.springboard.content;

import java.util.HashMap;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.v4.database.DatabaseUtilsCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.util.Map;

import uk.ac.cam.cl.ss958.springboard.content.SqlSchema.Column;
import uk.ac.cam.cl.ss958.springboard.content.SqlSchema.ColumnType;

public class DatabaseContentProvider  extends ContentProvider {
	private static final String TAG = "SpringBoard";
	
    public static final String AUTHORITY = "uk.ac.cam.cl.ss958.springboard.content";
	
	// A projection map used to select columns from the database
	private final Map<String, Map<String, String>> mProjectionMaps;
	// Uri matcher to decode incoming URIs.
	private final UriMatcher mUriMatcher;

	// Handle to a new DatabaseHelper.
	private ProvidingDatabase pdb;

	/**
	 * Global provider initialization.
	 */
	public DatabaseContentProvider() {
		List<SqlSchema> schemas = new SpringboardSqlSchema().getTables();
		// Create and initialize URI matcher.
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mProjectionMaps = new HashMap<String, Map<String,String>>();
	
		for (SqlSchema table : schemas) {
			mUriMatcher.addURI(AUTHORITY, table.getName(), 2*table.getId());
			mUriMatcher.addURI(AUTHORITY, table.getName() +"/*", 2*table.getId()-1);

			
			Map<String,String> projectionMap = new HashMap<String, String>();
			
			for (Column c : table.getColumns()) {
				projectionMap.put(c.getName(), c.getName());
			}
			
			if (TextUtils.isEmpty(table.getPrimaryKey())) {
				throw new IllegalArgumentException("Content provider requires primary key.");
			}
			
			if (!table.getPrimaryKey().equals("_id"))
					projectionMap.put(table.getPrimaryKey(), "_id");
			
			mProjectionMaps.put(table.getName(), projectionMap);
		}
	
	}

	/**
	 * Perform provider creation.
	 */
	@Override
	public boolean onCreate() {
		pdb = new SpringboardDatabaseHelper(getContext());
		return true;
	}

	/**
	 * Handle incoming queries.
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		// Constructs a new query builder and sets its table name
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		Pair<SqlSchema, String> decodedUri = decodeUri(uri);
		SqlSchema referencedTable = decodedUri.first;
		String rowId = decodedUri.second;
		
		qb.setTables(referencedTable.getName());
		qb.setProjectionMap(mProjectionMaps.get(referencedTable.getName()));
		
		if (rowId != null) {
			qb.appendWhere(referencedTable.getPrimaryKey() + "=?");
            selectionArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs,
                    new String[] { rowId });
		}

		if (TextUtils.isEmpty(sortOrder)) {
			sortOrder = referencedTable.getDefaultSortOrder();
		}

		SQLiteDatabase db = pdb.getDBHelper().getReadableDatabase();

		Cursor c = qb.query(db, projection, selection, selectionArgs,
				null /* no group */, null /* no filter */, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	private Pair<SqlSchema,String> decodeUri(Uri uri) {
		SqlSchema referencedTable = null;
		String rowId = null;
		int matchedId = mUriMatcher.match(uri);
		for (SqlSchema table : pdb.getSchemasForProvider()) {
			if (matchedId == 2*table.getId()) {
				referencedTable = table;
			} else if (matchedId == 2*table.getId()-1) {
				referencedTable = table;
				rowId = uri.getLastPathSegment();
			}
		}
		if (referencedTable == null) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return new Pair<SqlSchema,String>(referencedTable, rowId);
	}
	
	/**
	 * Return the MIME type for an known URI in the provider.
	 */
	@Override
	public String getType(Uri uri) {
		  
		  SqlSchema table = decodeUri(uri).first;
		  return "vnd.android.cursor.dir/" +
		  		 table.getContentType();
	}

	/**
	 * Handler inserting new data.
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		Pair<SqlSchema, String> decodedUri = decodeUri(uri);
		SqlSchema referencedTable = decodedUri.first;
		String rowId = decodedUri.second;

		if (!TextUtils.isEmpty(rowId)) {
			throw new IllegalArgumentException("Insert cannot be called with row uri");
		}
		
		ContentValues values;

		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		for (Column column : referencedTable.getColumns()) {
			if (column.getName().equals(referencedTable.getPrimaryKey()) &&
				column.getType() == SqlSchema.ColumnType.INTEGER) {
				// integer primary key can be autogenerated
				continue;
			}
			if (!values.containsKey(column.getName())) {
				values.put(column.getName(), column.defaultValue());
			}
		}

		SQLiteDatabase db = pdb.getDBHelper().getWritableDatabase();

		long newRowId = db.insert(referencedTable.getName(), null, values);

		// If the insert succeeded, the row ID exists.
		if (newRowId > 0) {
			// TODO: implement rows
			Uri noteUri = Uri.parse("content://" + 
									AUTHORITY + "/" + 
									referencedTable.getName() + "/" +
									newRowId);
			getContext().getContentResolver().notifyChange(noteUri, null);
			return noteUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
		//return null;
	}

	/**
	 * Handle deleting data.
	 */
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = pdb.getDBHelper().getWritableDatabase();

		int count;

		Pair<SqlSchema, String> decodedUri = decodeUri(uri);
		SqlSchema referencedTable = decodedUri.first;
		String rowId = decodedUri.second;
		
		if (rowId != null) {
			where = DatabaseUtilsCompat.concatenateWhere(
					referencedTable.getPrimaryKey() + " = " +
					rowId, where);	
		}
		count = db.delete(referencedTable.getName(), where, whereArgs);		
		

		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	/**Fragment.java:394
	 * Handle updating data.
	 */
	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		/*SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		String finalWhere;

		switch (mUriMatcher.match(uri)) {
		case MAIN:
			// If URI is main table, update uses incoming where clause and args.
			count = db.update(MainTable.TABLE_NAME, values, where, whereArgs);
			break;

		case MAIN_ID:
			// If URI is for a particular row ID, update is based on incoming
			// data but modified to restrict to the given ID.
			finalWhere = DatabaseUtilsCompat.concatenateWhere(
					MainTable._ID + " = " + ContentUris.parseId(uri), where);
			count = db.update(MainTable.TABLE_NAME, values, finalWhere, whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return count;*/
		return 0;
	}
}