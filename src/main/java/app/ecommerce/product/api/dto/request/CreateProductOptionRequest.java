package app.ecommerce.product.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateProductOptionRequest(
    @NotBlank(message = "Option name must not be blank")
    @Size(max = 255, message = "Option name must not exceed 255 characters")
    String optionName,

    @NotEmpty(message = "At least one option value is required")
    List<
        @NotBlank(message = "Option value must not be blank")
        @Size(max = 255, message = "Option value must not exceed 255 characters")
        String> values
) {
}
