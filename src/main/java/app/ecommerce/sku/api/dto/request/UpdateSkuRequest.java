package app.ecommerce.sku.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateSkuRequest(
    @NotNull(message = "Weight must not be null")
    @Positive(message = "Weight must be greater than 0")
    Integer weightGrams,

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.00", message = "Amount must not be negative")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer and 2 fraction digits")
    BigDecimal amount,

    @NotBlank(message = "Currency must not be blank")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter uppercase ISO code")
    String currency
) {
}
