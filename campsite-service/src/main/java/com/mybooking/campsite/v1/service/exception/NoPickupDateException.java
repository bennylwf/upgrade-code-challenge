package com.mybooking.campsite.v1.service.exception;

public class NoPickupDateException extends RuntimeException {
    public NoPickupDateException(String msg) {
        super(msg);
    }
}
