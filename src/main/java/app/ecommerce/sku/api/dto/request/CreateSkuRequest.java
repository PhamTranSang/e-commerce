package app.ecommerce.sku.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSkuRequest(
    @NotNull(message = "Product id must not be null")
    UUID productId,

    @NotBlank(message = "SKU code must not be blank")
    @Size(max = 64, message = "SKU code must not exceed 64 characters")
    String skuCode,

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.00", message = "Amount must not be negative")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer and 2 fraction digits")
    BigDecimal amount,

    @NotBlank(message = "Currency must not be blank")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter uppercase ISO code")
    String currency,

    @NotNull(message = "Weight must not be null")
    @Positive(message = "Weight must be greater than 0")
    Integer weightGrams,

    /** Exactly one value per option of the product (empty for a product with no options). */
    List<@NotNull UUID> optionValueIds
) {
}
