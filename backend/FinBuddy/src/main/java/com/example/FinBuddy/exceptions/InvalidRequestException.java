package com.example.FinBuddy.exceptions;

/**
 * Exception thrown when a request contains invalid data
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
