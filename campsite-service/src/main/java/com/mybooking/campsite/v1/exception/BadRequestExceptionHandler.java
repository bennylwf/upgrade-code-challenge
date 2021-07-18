package com.mybooking.campsite.v1.exception;


import com.mybooking.campsite.v1.rest.spec.model.BadRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@ControllerAdvice
public class BadRequestExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(value = {BadRequestException.class})
	protected ResponseEntity<BadRequest> handleBadRequestException(
			RuntimeException ex, WebRequest request) {
		BadRequest badRequest = ((BadRequestException) ex).getBadRequest();

		return new ResponseEntity<>(
				badRequest, new HttpHeaders(), ((BadRequestException) ex).getStatus());
	}

}
