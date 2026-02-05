package com.example.FinBuddy.exceptions;

/**
 * Exception thrown when an external service (like stock price API) fails
 */
public class ExternalServiceException extends RuntimeException {

    private String serviceName;

    public ExternalServiceException(String serviceName, String message) {
        super(String.format("External service '%s' error: %s", serviceName, message));
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(String.format("External service '%s' error: %s", serviceName, message), cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
