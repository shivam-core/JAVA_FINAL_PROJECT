package com.shivamcore.loanapp.exception;

/**
 * Thrown when a user attempts an administrative action without ROLE_ADMIN.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
