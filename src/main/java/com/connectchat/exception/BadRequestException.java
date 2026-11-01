package com.connectchat.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown on invalid request parameters or payload constraints.
 */
public class BadRequestException extends AppException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}

