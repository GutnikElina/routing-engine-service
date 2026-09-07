package com.logistics.routing.domain.vrp.exception;

public class VrpSolverException extends RuntimeException {

    public VrpSolverException(String message) {
        super(message);
    }

    public VrpSolverException(String message, Throwable cause) {
        super(message, cause);
    }
}
