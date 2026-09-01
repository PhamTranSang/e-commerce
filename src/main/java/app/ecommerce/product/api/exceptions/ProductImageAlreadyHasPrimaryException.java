package app.ecommerce.product.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceConflictException;
import java.util.UUID;

public final class ProductImageAlreadyHasPrimaryException extends ResourceConflictException {

    public static final String CODE = "PRODUCT_IMAGE_ALREADY_HAS_PRIMARY";

    public ProductImageAlreadyHasPrimaryException(final UUID productId) {
        this(productId, null);
    }

    public ProductImageAlreadyHasPrimaryException(final UUID productId, final Throwable cause) {
        super(CODE, "Product '%s' already has a primary image".formatted(productId), cause);
    }
}
