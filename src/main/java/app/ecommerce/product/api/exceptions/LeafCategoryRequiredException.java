package app.ecommerce.product.api.exceptions;

import app.ecommerce.shared.api.exceptions.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Raised when a product is attached to a category that has sub-categories.
 * Products may only be attached to a leaf category (one with no children).
 */
public final class LeafCategoryRequiredException extends BusinessException {

    public static final String CODE = "LEAF_CATEGORY_REQUIRED";

    public LeafCategoryRequiredException(final UUID categoryId) {
        super(
            HttpStatus.CONFLICT,
            CODE,
            "Category '%s' has sub-categories; products must be attached to a leaf category"
                .formatted(categoryId)
        );
    }
}
