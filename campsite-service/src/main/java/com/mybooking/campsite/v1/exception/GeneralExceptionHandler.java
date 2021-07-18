package com.mybooking.campsite.v1.exception;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@ControllerAdvice
public class GeneralExceptionHandler extends ResponseEntityExceptionHandler {

	private final Logger logger = LoggerFactory.getLogger( this.getClass() );

	@ExceptionHandler(value = {RuntimeException.class})
	protected ResponseEntity<String> handleGeneralException(
			RuntimeException ex, WebRequest request) {

		logger.error("Error found", ex);
		return new ResponseEntity<String>("errorCode: 99", HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
