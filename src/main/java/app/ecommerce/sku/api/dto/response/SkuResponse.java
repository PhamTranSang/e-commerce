package app.ecommerce.sku.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SkuResponse(
    UUID skuId,
    UUID productId,
    String skuCode,
    BigDecimal amount,
    String currency,
    Integer weightGrams,
    List<UUID> optionValueIds,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
