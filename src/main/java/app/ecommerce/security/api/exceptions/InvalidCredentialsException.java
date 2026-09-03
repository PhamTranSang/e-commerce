package app.ecommerce.security.api.exceptions;

import app.ecommerce.shared.api.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Login failed. The message is deliberately generic so it does not reveal whether the
 * login exists or the password was wrong.
 */
public final class InvalidCredentialsException extends BusinessException {

    public static final String CODE = "INVALID_CREDENTIALS";

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, CODE, "Invalid login credentials");
    }
}
