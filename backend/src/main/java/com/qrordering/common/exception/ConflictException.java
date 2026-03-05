package com.qrordering.common.exception;

/**
 * Exception for conflict (e.g. optimistic lock failure). Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
