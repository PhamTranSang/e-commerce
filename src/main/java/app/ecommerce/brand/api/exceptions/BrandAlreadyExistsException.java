package app.ecommerce.brand.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceConflictException;

public final class BrandAlreadyExistsException extends ResourceConflictException {

    public static final String CODE = "BRAND_ALREADY_EXISTS";

    public BrandAlreadyExistsException(final String brandName) {
        this(brandName, null);
    }

    public BrandAlreadyExistsException(final String brandName, final Throwable cause) {
        super(CODE, "Brand '%s' already exists".formatted(brandName), cause);
    }
}
