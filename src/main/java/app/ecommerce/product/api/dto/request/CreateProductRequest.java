package app.ecommerce.product.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProductRequest(
    @NotNull(message = "Category id must not be null")
    UUID categoryId,

    @NotNull(message = "Brand id must not be null")
    UUID brandId,

    @NotBlank(message = "Product name must not be blank")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    String productName,

    @Size(max = 5000, message = "Product description must not exceed 5000 characters")
    String productDescription
) {
}
