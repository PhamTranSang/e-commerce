package app.ecommerce.product.api.service;

import app.ecommerce.product.api.dto.request.CreateProductImageRequest;
import app.ecommerce.product.api.dto.response.ProductImageResponse;
import java.util.List;
import java.util.UUID;

public interface ProductImageService {

    ProductImageResponse addImage(final UUID productId, final CreateProductImageRequest request);

    List<ProductImageResponse> getProductImages(final UUID productId);
}
