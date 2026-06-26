package com.kaptaitourist.kaptaitourist.core.exception;

/** Thrown when a request conflicts with existing state (e.g. duplicate email/mobile). Maps to 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
