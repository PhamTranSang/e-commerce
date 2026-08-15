package app.ecommerce.shared.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(final HttpStatus status, final String code, final String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}