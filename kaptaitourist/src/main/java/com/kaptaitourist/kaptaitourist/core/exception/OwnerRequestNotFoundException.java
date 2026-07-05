package com.kaptaitourist.kaptaitourist.core.exception;

/** Thrown when an owner-enlistment request does not exist. Maps to 404. */
public class OwnerRequestNotFoundException extends RuntimeException {
    public OwnerRequestNotFoundException(String message) {
        super(message);
    }
}
