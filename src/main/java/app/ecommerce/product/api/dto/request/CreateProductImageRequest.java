package app.ecommerce.product.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProductImageRequest(
    @NotBlank(message = "Image url must not be blank")
    @Size(max = 1024, message = "Image url must not exceed 1024 characters")
    String url,

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    String altText,

    /** Optional: tie this image to a specific option value (e.g. the colour "Blue"). */
    UUID optionValueId,

    /** Whether this image is the product's primary image. Defaults to false when omitted. */
    Boolean isPrimary
) {
}
