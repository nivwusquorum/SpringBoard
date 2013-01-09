package uk.ac.cam.cl.ss958.huggler;

import java.util.Date;

import java.io.Serializable;
import java.text.DateFormat;

public class ChatMessage implements Serializable{
	private static final long serialVersionUID = 1L;

	private static final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);

	private String user;
	private String message;
	private Date timestamp;
	
	
	public String getUser() {
		return user;
	}

	public String getMessage() {
		return message;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	
	public ChatMessage(ChatMessage copy) {
		user = copy.user;
		message = copy.message;
		timestamp = copy.timestamp;
	}
	
	public ChatMessage(String user,
					   String message) {
		this.user = user;
		this.message = message;
		this.timestamp = new Date();
	}

	public ChatMessage(String user,
				       String message,
				       java.util.Date date) {
		this.user = user;
		this.message = message;
		this.timestamp = date;
	}
	
	public String toString() {
		return "At " + df.format(timestamp) + " " + user + " wrote: " + message;
	}
}
