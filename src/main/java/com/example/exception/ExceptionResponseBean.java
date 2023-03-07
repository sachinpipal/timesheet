package com.example.exception;

import java.util.Date;

public class ExceptionResponseBean {

	private Date timestamp;
	
	private String message;
	
	private String description;

	/**
	 * 
	 */
	public ExceptionResponseBean() {
		super();
	}

	public ExceptionResponseBean(Date timestamp, String message, String description) {
		super();
		this.timestamp = timestamp;
		this.message = message;
		this.description = description;
	}

	public Date getTimeStamp() {
		return timestamp;
	}

	public void setTimeStamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
	
}
