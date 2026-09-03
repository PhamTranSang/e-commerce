package app.ecommerce.security.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceNotFoundException;
import java.util.UUID;

public final class AccountNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "ACCOUNT_NOT_FOUND";

    public AccountNotFoundException(final UUID accountId) {
        super(CODE, "Account '%s' was not found".formatted(accountId), null);
    }
}
