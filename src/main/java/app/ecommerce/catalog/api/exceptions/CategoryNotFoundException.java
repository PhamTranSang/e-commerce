package app.ecommerce.catalog.api.exceptions;

import app.ecommerce.shared.api.exceptions.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends BusinessException {

    public static final String CODE = "CATEGORY_NOT_FOUND";

    public CategoryNotFoundException(final UUID categoryId) {
        this(categoryId, null);
    }

    public CategoryNotFoundException(final UUID categoryId, final Throwable cause) {
        super(
            HttpStatus.NOT_FOUND,
            CODE,
            "Category '%s' was not found".formatted(categoryId),
            cause
        );
    }
}