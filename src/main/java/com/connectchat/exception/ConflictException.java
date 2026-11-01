package com.connectchat.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown on unique constraint collisions (duplicate username or email).
 */
public class ConflictException extends AppException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

