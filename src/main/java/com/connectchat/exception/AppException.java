package com.connectchat.exception;

import org.springframework.http.HttpStatus;

/**
 * Base custom application runtime exception.
 */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final HttpStatus status;

    public AppException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
