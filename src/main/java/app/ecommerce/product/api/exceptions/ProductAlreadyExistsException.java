package app.ecommerce.product.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceConflictException;

public final class ProductAlreadyExistsException extends ResourceConflictException {

    public static final String CODE = "PRODUCT_ALREADY_EXISTS";

    public ProductAlreadyExistsException(final String productName) {
        this(productName, null);
    }

    public ProductAlreadyExistsException(final String productName, final Throwable cause) {
        super(CODE, "Product '%s' already exists in this category".formatted(productName), cause);
    }
}
