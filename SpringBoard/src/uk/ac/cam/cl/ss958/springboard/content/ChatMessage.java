package uk.ac.cam.cl.ss958.springboard.content;

import java.util.Date;

import java.io.Serializable;
import java.text.DateFormat;

import android.content.ContentValues;

public class ChatMessage implements Serializable{
	private static final long serialVersionUID = 1L;

	private static final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);

	private final String user;
	private final int	msgid; 
	private final String message;
	private final long timestamp;
	private final String target;
	
	public String getUser() {
		return user;
	}

	public String getMessage() {
		return message;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public int getMsgId() {
		return msgid;
	}
	
	public String getTarget() {
		return target;
	}
	
	public ChatMessage(ChatMessage copy) {
		user = copy.user;
		message = copy.message;
		timestamp = copy.timestamp;
		target = copy.target;
		msgid = copy.msgid;
		
	}
	
	public ChatMessage(String user,
					   String message,
					   long timestamp,
					   int msgid,
					   String target) {
		this.user = user;
		this.message = message;
		this.timestamp = timestamp;
		this.msgid = msgid;
		this.target = target;
	}
	
	public ContentValues toContentValues() {
		ContentValues cv = new ContentValues();
		cv.put(SpringboardSqlSchema.Strings.Messages.KEY_USER, user);
		cv.put(SpringboardSqlSchema.Strings.Messages.KEY_MSGID, msgid);
		cv.put(SpringboardSqlSchema.Strings.Messages.KEY_MESSAGE, message);
		cv.put(SpringboardSqlSchema.Strings.Messages.KEY_TIMESTAMP, timestamp);
		cv.put(SpringboardSqlSchema.Strings.Messages.KEY_TARGET, target);
		return cv;
	}
	
	public String toString() {
		return "At " + df.format(new Date(timestamp)) + " " + user + " wrote: " + message;
	}
}
