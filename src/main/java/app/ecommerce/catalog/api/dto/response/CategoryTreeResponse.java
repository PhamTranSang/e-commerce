package app.ecommerce.catalog.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * A category node with its descendants nested underneath.
 * A leaf node carries an empty {@code children} list.
 */
public record CategoryTreeResponse(
    UUID categoryId,
    String categoryName,
    List<CategoryTreeResponse> children
) {
}
