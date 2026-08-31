package app.ecommerce.sku.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SkuResponse(
    UUID skuId,
    UUID productId,
    String skuCode,
    Integer weightGrams,
    BigDecimal amount,
    String currency,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
