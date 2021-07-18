package com.mybooking.campsite.v1.exception;

import com.mybooking.campsite.v1.rest.spec.model.BadRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


public class BadRequestException extends ResponseStatusException {

	private BadRequest badRequest;

	public BadRequestException(HttpStatus status, BadRequest badRequest) {
		super(status, null, null);
		this.badRequest = badRequest;
	}

	public BadRequest getBadRequest() {
		return this.badRequest;
	}
}
