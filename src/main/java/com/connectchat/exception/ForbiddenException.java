package com.connectchat.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts an operation on a resource they do not own
 * or are not a member of (e.g. non-member sending messages to a conversation).
 */
public class ForbiddenException extends AppException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}

