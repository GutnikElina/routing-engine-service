package com.logistics.routing.adapter.in.web.error;

import com.logistics.routing.domain.route.exception.InvalidRouteDraftException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class RouteExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidRouteDraftException.class)
    public ProblemDetail handleInvalidRouteDraft(
            InvalidRouteDraftException exception
    ) {
        log.atDebug().log("Invalid route draft: {}", exception.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
        problemDetail.setTitle("Invalid route draft");
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception exception) {
        log.atError().setCause(exception).log("Unexpected exception");

        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
