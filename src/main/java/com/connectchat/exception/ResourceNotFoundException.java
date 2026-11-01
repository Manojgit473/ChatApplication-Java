package com.connectchat.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an entity (User, Conversation, Message, etc.) is not found.
 */
public class ResourceNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue), HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}

