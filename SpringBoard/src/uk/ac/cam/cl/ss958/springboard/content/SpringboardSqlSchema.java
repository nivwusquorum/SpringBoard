package uk.ac.cam.cl.ss958.springboard.content;

import java.util.ArrayList;
import java.util.List;

public class SpringboardSqlSchema {
	
	public static final String DATABASE_NAME = "SpringBoard";
	public static final int DATABASE_VERSION = 4;
	
	List<SqlSchema> schemas;
	
	public static class Strings {
		public static class Messages {
			public static String NAME = "messages";
			public static String KEY_ID = "_id";
			public static String KEY_MESSAGE = "message";
		}
		
		public static class Properties {
			public static String NAME = "properties";
			public static String KEY_ID = "_id";
			public static String KEY_NAME = "name";
			public static String KEY_VALUE = "value";
			
			public static String P_USERNAME = "username";
		}
	}


	public enum Table {
		MESSAGES (new SqlSchema() {
			
			@Override
			public int getId() {
				return 1;
			}

			@Override
			public String getName() {
				return Strings.Messages.NAME;
			}

			public String getPrimaryKey() {
				return Strings.Messages.KEY_ID;
			}

			@Override
			public List<Column> getColumns() {
				List<Column> columns = new ArrayList<Column>();
				columns.add(new Column(Strings.Messages.KEY_ID, ColumnType.INTEGER));
				columns.add(new Column(Strings.Messages.KEY_MESSAGE, ColumnType.TEXT));
				return columns;
			}

			@Override
			public String getContentType() {
				return "vnd.ss958.message-table";
			}
			
		}),
		PROPERTIES (new SqlSchema() {
			
			@Override
			public int getId() {
				return 2;
			}

			@Override
			public String getName() {
				return Strings.Properties.NAME;
			}

			public String getPrimaryKey() {
				return Strings.Properties.KEY_ID;
			}

			@Override
			public List<Column> getColumns() {
				List<Column> columns = new ArrayList<Column>();
				columns.add(new Column(Strings.Properties.KEY_ID, ColumnType.INTEGER));
				columns.add(new Column(Strings.Properties.KEY_NAME, ColumnType.TEXT));
				columns.add(new Column(Strings.Properties.KEY_VALUE, ColumnType.TEXT));
				return columns;
			}

			@Override
			public String getContentType() {
				return "vnd.ss958.properties-table";
			}
			
		});
		
		SqlSchema schema;
		
		Table(SqlSchema schema) {
			this.schema = schema;
		}
		
		SqlSchema get() {
			return schema;
		}
		
	}
	
	
	public List<SqlSchema> getTables() {
		List<SqlSchema> ret = new ArrayList<SqlSchema>();
		for (Table t: Table.values()) {
			ret.add(t.get());
		}
		return ret;
	}
	
	public List<SqlSchema> getTablesForDataProvider() {
		List<SqlSchema> ret = new ArrayList<SqlSchema>();
		ret.add(Table.MESSAGES.get());
		return ret;
	}
}
