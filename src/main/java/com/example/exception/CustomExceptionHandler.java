package com.example.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;

@RestControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler{

	private static final String ERROR = "error";
	private static final String MESSAGE = "message";
	private static final String DESCRIPTION = "description";
	private static final String DOT = ".";

	@Autowired
	Environment env;
	
	@ExceptionHandler(Exception.class)
	public final ResponseEntity<Object> handleAllException(Exception ex, WebRequest request){
		ExceptionResponseBean exceptionResponseBean = new ExceptionResponseBean(new Date(),ex.getMessage(),request.getDescription(false));
		return new ResponseEntity(exceptionResponseBean,HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@ExceptionHandler(BillingCannotBeGeneratedException.class)
	public final ResponseEntity<Object> handleBillingCannotBeGeneratedException(Exception ex, WebRequest request){
		ExceptionResponseBean exceptionResponseBean = new ExceptionResponseBean(new Date(),"Unable to Generate Vendor Prefix",request.getDescription(false));
		return new ResponseEntity(exceptionResponseBean,HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(InvalidFileFormatException.class)
	public final ResponseEntity<Object> handleInvalidFileFormatException(Exception ex, WebRequest request){
		ExceptionResponseBean exceptionResponseBean = new ExceptionResponseBean(new Date(),"Duplicate Vendor Name",request.getDescription(false));
		return new ResponseEntity(exceptionResponseBean,HttpStatus.BAD_REQUEST);
	}
	@ExceptionHandler(IncorrectSAPDataException.class)
	public final ResponseEntity<Object> handleIncorrectSAPDataException(Exception ex, WebRequest request){
		ExceptionResponseBean exceptionResponseBean = new ExceptionResponseBean(new Date(),"Invalid Vendor Request",request.getDescription(false));
		return new ResponseEntity(exceptionResponseBean,HttpStatus.BAD_REQUEST);
	}
	@ExceptionHandler(IncorrectWandDataException.class)
	public final ResponseEntity<Object> handleIncorrectWandDataException(Exception ex, WebRequest request){
		ExceptionResponseBean exceptionResponseBean = new ExceptionResponseBean(new Date(),"Invalid Vendor Request",request.getDescription(false));
		return new ResponseEntity(exceptionResponseBean,HttpStatus.BAD_REQUEST);
	}
	@ExceptionHandler(OutputUploadException.class)
	public final ResponseEntity<Object> handleOutputUploadException(Exception ex, WebRequest request){
		ExceptionResponseBean exceptionResponseBean = new ExceptionResponseBean(new Date(),"Invalid Vendor Request",request.getDescription(false));
		return new ResponseEntity(exceptionResponseBean,HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler({ IllegalArgumentException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
		Error error = getErrorMessage(HttpStatus.BAD_REQUEST.value(), 28);
		error.setDescription(ex.getMessage());
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		ExceptionResponseBean exceptionResponseBean = new ExceptionResponseBean(new Date(),"Invalid Body",ex.getMessage());
		return new ResponseEntity(exceptionResponseBean,status);
	}
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		ExceptionResponseBean exceptionResponseBean = new ExceptionResponseBean(new Date(),"Invalid Request Body",ex.getBindingResult().getFieldError().toString());
		return new ResponseEntity(exceptionResponseBean,HttpStatus.BAD_REQUEST);
	}
	
	private Error getErrorMessage(int status, int code) {
		Error error = new Error();
		if (env.getProperty(ERROR + DOT + status + DOT + MESSAGE + DOT + code) != null) {
			error.setCode(code);
			error.setMessage(env.getProperty(ERROR + DOT + status + DOT + MESSAGE + DOT + code));
			error.setDescription(env.getProperty(ERROR + DOT + status + DOT + DESCRIPTION + DOT + code));
		} else {
			error = getErrorMessage(500, 1);
		}
		return error;
	}

}

