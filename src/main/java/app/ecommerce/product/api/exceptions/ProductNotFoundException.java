package app.ecommerce.product.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceNotFoundException;
import java.util.UUID;

public final class ProductNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "PRODUCT_NOT_FOUND";

    public ProductNotFoundException(final UUID productId) {
        this(productId, null);
    }

    public ProductNotFoundException(final UUID productId, final Throwable cause) {
        super(CODE, "Product '%s' was not found".formatted(productId), cause);
    }
}
