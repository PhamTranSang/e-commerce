package app.ecommerce.shared.api.exceptions;

import org.springframework.http.HttpStatus;

public abstract class ResourceNotFoundException extends BusinessException {

    protected ResourceNotFoundException(final String code, final String message) {
        this(code, message, null);
    }

    protected ResourceNotFoundException(
        final String code,
        final String message,
        final Throwable cause
    ) {
        super(HttpStatus.NOT_FOUND, code, message, cause);
    }
}