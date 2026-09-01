package app.ecommerce.product.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID productId,
    UUID categoryId,
    UUID brandId,
    String productName,
    String productDescription,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
