package com.kaptaitourist.kaptaitourist.core.exception;

public class HotelNotFoundException extends RuntimeException {
    public HotelNotFoundException(String message) {
        super(message);
    }
}
