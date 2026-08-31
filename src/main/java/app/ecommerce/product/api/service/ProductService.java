package app.ecommerce.product.api.service;

import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.product.api.dto.request.UpdateProductRequest;
import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.product.api.dto.response.ProductResponse;
import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(final CreateProductRequest request);

    ProductResponse getProduct(final UUID productId);

    PageResponse<ProductResponse> getProducts(final UUID categoryId, final int page, final int size);

    ProductResponse updateProduct(final UUID productId, final UpdateProductRequest request);

    void deactivateProduct(final UUID productId);
}
