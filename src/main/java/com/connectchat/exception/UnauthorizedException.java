package com.connectchat.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when unauthenticated user attempts to access protected operations.
 */
public class UnauthorizedException extends AppException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

