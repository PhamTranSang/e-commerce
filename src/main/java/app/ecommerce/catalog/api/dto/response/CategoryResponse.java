package app.ecommerce.catalog.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
    UUID categoryId,
    String categoryName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}