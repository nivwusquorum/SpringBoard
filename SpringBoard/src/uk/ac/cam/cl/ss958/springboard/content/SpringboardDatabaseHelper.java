package uk.ac.cam.cl.ss958.springboard.content;

import java.util.List;
import java.util.Random;

import uk.ac.cam.cl.ss958.springboard.content.SpringboardSqlSchema.Strings;
import uk.ac.cam.cl.ss958.springboard.content.SqlSchema.Column;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;


public class SpringboardDatabaseHelper extends SQLiteOpenHelper implements ProvidingDatabase {

	
	SpringboardSqlSchema tables = new SpringboardSqlSchema();
	
	public SpringboardDatabaseHelper(Context c) {
		super(c, SpringboardSqlSchema.DATABASE_NAME, 
				 null, 
				 SpringboardSqlSchema.DATABASE_VERSION );
	}

	

	@Override
	public void onCreate(SQLiteDatabase db) {
		for(SqlSchema table : tables.getTables()) {
			String sql = "CREATE TABLE " + table.getName() + "(";
			boolean first_pass = true;
			for(Column c : table.getColumns()) {
				if (first_pass) {
					first_pass = false;
				} else {
					sql += ",";
				}
				sql += c.sql();
			}
			if (!TextUtils.isEmpty(table.getPrimaryKey())) {
				sql += ", PRIMARY KEY (" + table.getPrimaryKey() + ")" ;
			}
			sql += ")";
			db.execSQL(sql);
		}
		initValues(db);
	}

	private void initValues(SQLiteDatabase db) {
		ContentValues cv = new ContentValues();
		cv.put(SpringboardSqlSchema.Strings.Properties.KEY_NAME, 
			   SpringboardSqlSchema.Strings.Properties.P_USERNAME);
	    Random generator = new Random();
        String randomId = "sb_user" + generator.nextInt(1000);
		cv.put(SpringboardSqlSchema.Strings.Properties.KEY_VALUE, 
			   randomId);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
		for(SqlSchema table : tables.getTables()) {
			// Kills the table and existing data
			db.execSQL("DROP TABLE IF EXISTS " + table.getName());
		}

        // Recreates the database with a new version
        onCreate(db);
		
	}



	@Override
	public SQLiteOpenHelper getDBHelper() {
		return this;
	}



	@Override
	public List<SqlSchema> getSchemasForProvider() {
		return tables.getTablesForDataProvider();
	}

}
