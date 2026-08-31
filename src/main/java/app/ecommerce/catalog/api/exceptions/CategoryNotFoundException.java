package app.ecommerce.catalog.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceNotFoundException;
import java.util.UUID;

public final class CategoryNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "CATEGORY_NOT_FOUND";

    public CategoryNotFoundException(final UUID categoryId) {
        this(categoryId, null);
    }

    public CategoryNotFoundException(final UUID categoryId, final Throwable cause) {
        super(CODE, "Category '%s' was not found".formatted(categoryId), cause);
    }
}
