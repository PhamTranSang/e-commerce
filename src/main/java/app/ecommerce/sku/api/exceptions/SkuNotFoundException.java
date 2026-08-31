package app.ecommerce.sku.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceNotFoundException;
import java.util.UUID;

public final class SkuNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "SKU_NOT_FOUND";

    public SkuNotFoundException(final UUID skuId) {
        this(skuId, null);
    }

    public SkuNotFoundException(final UUID skuId, final Throwable cause) {
        super(CODE, "SKU '%s' was not found".formatted(skuId), cause);
    }
}
