package app.ecommerce.shared.api.exceptions;

import org.springframework.http.HttpStatus;

public abstract class ResourceConflictException extends BusinessException {

    protected ResourceConflictException(final String code, final String message) {
        this(code, message, null);
    }

    protected ResourceConflictException(
        final String code,
        final String message,
        final Throwable cause
    ) {
        super(HttpStatus.CONFLICT, code, message, cause);
    }
}