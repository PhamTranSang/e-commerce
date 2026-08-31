package app.ecommerce.product.impl.mapper;

import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.product.api.dto.request.UpdateProductRequest;
import app.ecommerce.product.api.dto.response.ProductResponse;
import app.ecommerce.catalog.impl.entity.CategoryEntity;
import app.ecommerce.product.impl.entity.ProductEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductEntity toNewEntity(
        final CreateProductRequest request,
        final CategoryEntity category,
        final Instant now
    ) {
        final var entity = new ProductEntity();
        entity.setCategory(category);
        entity.setProductName(request.productName());
        entity.setProductDescription(request.productDescription());
        entity.setIsActive(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public void update(
        final ProductEntity entity,
        final UpdateProductRequest request,
        final Instant updatedAt
    ) {
        entity.setProductName(request.productName());
        entity.setProductDescription(request.productDescription());
        entity.setUpdatedAt(updatedAt);
    }

    public void deactivate(final ProductEntity entity, final Instant updatedAt) {
        entity.setIsActive(false);
        entity.setUpdatedAt(updatedAt);
    }

    public ProductResponse toResponse(final ProductEntity entity) {
        return new ProductResponse(
            entity.getProductId(),
            entity.getCategory().getCategoryId(),
            entity.getProductName(),
            entity.getProductDescription(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}