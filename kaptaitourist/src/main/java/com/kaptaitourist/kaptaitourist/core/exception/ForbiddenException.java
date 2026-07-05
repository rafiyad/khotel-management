package com.kaptaitourist.kaptaitourist.core.exception;

/** Thrown when an authenticated caller is not allowed to act on a resource (resource-level). Maps to 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
