package com.logistics.routing.adapter.in.web.error;

import com.logistics.routing.domain.route.exception.InvalidRouteDraftException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class RouteExceptionHandler {

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
        problem.setTitle("Invalid request");

        List<Map<String, String>> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()
                ))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception exception) {
        log.atError().setCause(exception).log("Unexpected exception");

        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
