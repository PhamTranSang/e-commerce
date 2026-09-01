package app.ecommerce.brand.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceNotFoundException;
import java.util.UUID;

public final class BrandNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "BRAND_NOT_FOUND";

    public BrandNotFoundException(final UUID brandId) {
        this(brandId, null);
    }

    public BrandNotFoundException(final UUID brandId, final Throwable cause) {
        super(CODE, "Brand '%s' was not found".formatted(brandId), cause);
    }
}
