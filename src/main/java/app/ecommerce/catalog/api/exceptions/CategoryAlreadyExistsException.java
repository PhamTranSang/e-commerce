package app.ecommerce.catalog.api.exceptions;

import app.ecommerce.shared.api.exceptions.ResourceConflictException;

public final class CategoryAlreadyExistsException extends ResourceConflictException {

    public static final String CODE = "CATEGORY_ALREADY_EXISTS";

    public CategoryAlreadyExistsException(final String categoryName) {
        this(categoryName, null);
    }

    public CategoryAlreadyExistsException(final String categoryName, final Throwable cause) {
        super(CODE, "Category '%s' already exists under the same parent".formatted(categoryName), cause);
    }
}
