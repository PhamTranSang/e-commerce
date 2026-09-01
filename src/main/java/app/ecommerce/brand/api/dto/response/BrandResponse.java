package app.ecommerce.brand.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BrandResponse(
    UUID brandId,
    String brandName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
