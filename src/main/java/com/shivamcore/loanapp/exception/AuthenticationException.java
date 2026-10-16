package com.shivamcore.loanapp.exception;

/**
 * Exception thrown when authentication fails (invalid credentials, unknown user, locked account).
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
