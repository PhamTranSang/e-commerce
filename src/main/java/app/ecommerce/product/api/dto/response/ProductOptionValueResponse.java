package app.ecommerce.product.api.dto.response;

import java.util.UUID;

public record ProductOptionValueResponse(
    UUID optionValueId,
    String value,
    Integer position
) {
}
