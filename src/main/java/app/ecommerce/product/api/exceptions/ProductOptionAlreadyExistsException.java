package app.ecommerce.product.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceConflictException;

public final class ProductOptionAlreadyExistsException extends ResourceConflictException {

    public static final String CODE = "PRODUCT_OPTION_ALREADY_EXISTS";

    public ProductOptionAlreadyExistsException(final String optionName) {
        this(optionName, null);
    }

    public ProductOptionAlreadyExistsException(final String optionName, final Throwable cause) {
        super(CODE, "Option '%s' already exists for this product".formatted(optionName), cause);
    }
}
