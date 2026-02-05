package com.example.FinBuddy.exceptions;

/**
 * Exception thrown when user attempts to access resources they don't own
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String resourceName, Long resourceId) {
        super(String.format("Unauthorized access to %s with ID: %s", resourceName, resourceId));
    }
}
