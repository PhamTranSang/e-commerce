package app.ecommerce.sku.impl.mapper;

import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.impl.entity.SkuEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SkuMapper {

    public SkuEntity toNewEntity(
        final ProductEntity product,
        final CreateSkuRequest request,
        final String skuCode,
        final Instant now
    ) {
        final var entity = new SkuEntity();
        entity.setProduct(product);
        entity.setSkuCode(skuCode);
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency());
        entity.setWeightGrams(request.weightGrams());
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
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency());
        entity.setWeightGrams(request.weightGrams());
        entity.setUpdatedAt(updatedAt);
    }

    public void deactivate(final SkuEntity entity, final Instant updatedAt) {
        entity.setIsActive(false);
        entity.setUpdatedAt(updatedAt);
    }

    public SkuResponse toResponse(final SkuEntity entity) {
        final var optionValueIds = entity.getOptionValues().stream()
            .map(ProductOptionValueEntity::getOptionValueId)
            .toList();
        return new SkuResponse(
            entity.getSkuId(),
            entity.getProduct().getProductId(),
            entity.getSkuCode(),
            entity.getAmount(),
            entity.getCurrency(),
            entity.getWeightGrams(),
            optionValueIds,
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
