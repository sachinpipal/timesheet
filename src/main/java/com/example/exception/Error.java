package com.example.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(value = "Error")
public class Error {
	@JsonProperty("code")
	private Integer code = null;

	@JsonProperty("message")
	private String message = null;

	@JsonProperty("description")
	private String description = null;

	@JsonIgnore
	private String technicalError;

	public Error code(Integer code) {
		this.code = code;
		return this;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public Error message(String message) {
		this.message = message;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Error description(String description) {
		this.description = description;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTechnicalError() {
		return technicalError;
	}

	public void setTechnicalError(String technicalError) {
		this.technicalError = technicalError;
	}

	@Override
	public String toString() {
		return "Error [code=" + code + ", message=" + message + ", description=" + description + ", technicalError="
				+ technicalError + "]";
	}

}
