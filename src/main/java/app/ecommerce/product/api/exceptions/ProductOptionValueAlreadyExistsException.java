package app.ecommerce.product.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceConflictException;

public final class ProductOptionValueAlreadyExistsException extends ResourceConflictException {

    public static final String CODE = "PRODUCT_OPTION_VALUE_ALREADY_EXISTS";

    public ProductOptionValueAlreadyExistsException(final String value) {
        this(value, null);
    }

    public ProductOptionValueAlreadyExistsException(final String value, final Throwable cause) {
        super(CODE, "Duplicate option value '%s'".formatted(value), cause);
    }
}
