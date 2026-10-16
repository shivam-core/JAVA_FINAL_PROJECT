package com.shivamcore.loanapp.exception;

/**
 * Exception thrown when attempting to create or update a resource with a unique constraint conflict.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
