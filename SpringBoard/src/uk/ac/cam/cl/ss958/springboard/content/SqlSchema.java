package uk.ac.cam.cl.ss958.springboard.content;

import java.util.List;

public abstract class SqlSchema {
	public enum ColumnType {
		INTEGER("INTEGER"),
		TEXT("TEXT");
		
		String sql;
		ColumnType(String sql) {
			this.sql = sql;
		}
		
		public String defaultValue() {
			if (this.sql == "INTEGER") return "0";
			else if (this.sql == "TEXT") return "";
			return null;
		}
		
		public String sql() {
			return sql;
		}
	}
	
	public class Column {
		String name;
		ColumnType type;
		public Column(String name, ColumnType type) {
			this.name = name;
			this.type = type;
		}
		
		public String getName() {
			return name;
		}
		
		public ColumnType getType() {
			return type;
		}
		
		public String defaultValue() {
			return type.defaultValue();
		}
		
		public String sql() {
			return name +" " + type.sql();
		}
	}
	// Used for content provider.
	public abstract int getId(); 
	public abstract String getName();
	public abstract  String getPrimaryKey();
	public abstract List<Column> getColumns();
	
	public String getDefaultSortOrder() {
		return null;
	}
	
	public abstract String getContentType();
}
