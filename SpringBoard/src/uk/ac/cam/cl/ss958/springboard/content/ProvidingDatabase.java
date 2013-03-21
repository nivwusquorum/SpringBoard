package uk.ac.cam.cl.ss958.springboard.content;

import java.util.List;

import android.database.sqlite.SQLiteOpenHelper;

public interface ProvidingDatabase {
	SQLiteOpenHelper getDBHelper();
	List<SqlSchema> getSchemasForProvider();
}
