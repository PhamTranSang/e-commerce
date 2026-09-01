package app.ecommerce.product.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
    UUID imageId,
    UUID productId,
    UUID optionValueId,
    String url,
    String altText,
    Integer position,
    Boolean isPrimary,
    Instant createdAt,
    Instant updatedAt
) {
}
