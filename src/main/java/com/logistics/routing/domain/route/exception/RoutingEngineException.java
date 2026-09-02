package com.logistics.routing.domain.route.exception;

public class RoutingEngineException extends RuntimeException {

    public RoutingEngineException(String message) {
        super(message);
    }

    public RoutingEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
