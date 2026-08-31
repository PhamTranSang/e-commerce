package app.ecommerce.sku.impl.mapper;

import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.impl.entity.SkuEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SkuMapper {

    public SkuEntity toNewEntity(
        final CreateSkuRequest request,
        final ProductEntity product,
        final Instant now
    ) {
        final var entity = new SkuEntity();
        entity.setProduct(product);
        entity.setSkuCode(request.skuCode());
        entity.setWeightGrams(request.weightGrams());
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency());
        entity.setIsActive(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public void update(
        final SkuEntity entity,
        final UpdateSkuRequest request,
        final Instant updatedAt
    ) {
        entity.setWeightGrams(request.weightGrams());
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency());
        entity.setUpdatedAt(updatedAt);
    }

    public void deactivate(final SkuEntity entity, final Instant updatedAt) {
        entity.setIsActive(false);
        entity.setUpdatedAt(updatedAt);
    }

    public SkuResponse toResponse(final SkuEntity entity) {
        return new SkuResponse(
            entity.getSkuId(),
            entity.getProduct().getProductId(),
            entity.getSkuCode(),
            entity.getWeightGrams(),
            entity.getAmount(),
            entity.getCurrency(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
