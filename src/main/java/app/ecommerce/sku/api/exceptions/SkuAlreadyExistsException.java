package app.ecommerce.sku.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceConflictException;

public final class SkuAlreadyExistsException extends ResourceConflictException {

    public static final String CODE = "SKU_ALREADY_EXISTS";

    public SkuAlreadyExistsException(final String skuCode) {
        this(skuCode, null);
    }

    public SkuAlreadyExistsException(final String skuCode, final Throwable cause) {
        super(CODE, "SKU '%s' already exists".formatted(skuCode), cause);
    }
}
