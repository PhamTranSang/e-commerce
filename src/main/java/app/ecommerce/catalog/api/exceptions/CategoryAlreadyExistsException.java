package app.ecommerce.catalog.api.exceptions;

import app.ecommerce.shared.api.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public final class CategoryAlreadyExistsException extends BusinessException {

    public static final String CODE = "CATEGORY_ALREADY_EXISTS";

    public CategoryAlreadyExistsException(final String categoryName) {
        this(categoryName, null);
    }

    public CategoryAlreadyExistsException(final String categoryName, final Throwable cause) {
        super(
            HttpStatus.CONFLICT,
            CODE,
            "Category '%s' already exists".formatted(categoryName),
            cause
        );
    }
}
