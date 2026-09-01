package app.ecommerce.product.api.service;

import app.ecommerce.product.api.dto.request.CreateProductOptionRequest;
import app.ecommerce.product.api.dto.response.ProductOptionResponse;
import java.util.List;
import java.util.UUID;

public interface ProductOptionService {

    ProductOptionResponse addOption(final UUID productId, final CreateProductOptionRequest request);

    List<ProductOptionResponse> getProductOptions(final UUID productId);
}
