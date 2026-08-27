package app.ecommerce.shared.api.exceptions;

import app.ecommerce.shared.api.dto.response.ValidationError;
import app.ecommerce.shared.impl.filter.CorrelationIdFilter;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(final BusinessException e) {
        log.warn(
            "Business request rejected: code={}, status={}",
            e.getCode(),
            e.getStatus().value()
        );
        final var body = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
        body.setProperty("code", e.getCode());
        decorate(body);
        return body;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred. Please try again.");
        body.setProperty("code", "INTERNAL_ERROR");
        decorate(body);
        return body;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        log.debug(
            "Request validation failed: errorCount={}",
            ex.getBindingResult().getErrorCount()
        );
        final var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ValidationError(
                error.getField(),
                messageOrDefault(error.getDefaultMessage())
            ));
        final var objectErrors = ex.getBindingResult().getGlobalErrors().stream()
            .map(error -> new ValidationError(
                "request",
                messageOrDefault(error.getDefaultMessage())
            ));
        final var errors = Stream.concat(fieldErrors, objectErrors).toList();

        final var body = createValidationProblem(errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        final var errors = ex.getParameterValidationResults().stream()
            .flatMap(result -> {
                final var parameterName = result.getMethodParameter().getParameterName();
                final var field = parameterName == null ? "unknown" : parameterName;
                return result.getResolvableErrors().stream()
                    .map(error -> new ValidationError(
                        field,
                        messageOrDefault(error.getDefaultMessage())
                    ));
            })
            .toList();

        log.debug("Request parameter validation failed: errorCount={}", errors.size());

        final var body = createValidationProblem(errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            @NonNull TypeMismatchException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        log.debug("Path or request parameter has an invalid format");

        final var body = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "A path or request parameter has an invalid format."
        );
        body.setProperty("code", "INVALID_PATH_PARAMETER");
        decorate(body);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        log.debug("Request body could not be read");

        final var body = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request body is malformed or contains invalid data types."
        );
        body.setProperty("code", "MALFORMED_REQUEST_BODY");
        decorate(body);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    private ProblemDetail createValidationProblem(final List<ValidationError> errors) {
        final var body = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed."
        );
        body.setProperty("code", "VALIDATION_FAILED");
        body.setProperty("errors", errors);
        decorate(body);
        return body;
    }

    private String messageOrDefault(final String message) {
        return message == null ? "invalid" : message;
    }

    private void decorate(ProblemDetail body) {
        body.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}