package app.ecommerce.product.api.dto.response;

import java.util.List;
import java.util.UUID;

public record ProductOptionResponse(
    UUID optionId,
    String optionName,
    Integer position,
    List<ProductOptionValueResponse> values
) {
}
