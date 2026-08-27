package app.ecommerce.shared.api.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected BusinessException(final HttpStatus status, final String code, final String message) {
        this(status, code, message, null);
    }

    protected BusinessException(
        final HttpStatus status,
        final String code,
        final String message,
        final Throwable cause
    ) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
