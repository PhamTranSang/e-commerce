package app.ecommerce.brand.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameBrandRequest(
    @NotBlank(message = "Brand name must not be blank")
    @Size(max = 255, message = "Brand name must not exceed 255 characters")
    String brandName
) {
}
