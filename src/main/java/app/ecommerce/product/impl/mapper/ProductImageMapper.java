package app.ecommerce.product.impl.mapper;

import app.ecommerce.product.api.dto.response.ProductImageResponse;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.entity.ProductImageEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ProductImageMapper {

    public ProductImageEntity toNewEntity(
        final ProductEntity product,
        final ProductOptionValueEntity optionValue,
        final String url,
        final String altText,
        final int position,
        final boolean primary,
        final Instant now
    ) {
        final var entity = new ProductImageEntity();
        entity.setProduct(product);
        entity.setOptionValue(optionValue);
        entity.setUrl(url);
        entity.setAltText(altText);
        entity.setPosition(position);
        entity.setIsPrimary(primary);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public ProductImageResponse toResponse(final ProductImageEntity entity) {
        return new ProductImageResponse(
            entity.getImageId(),
            entity.getProduct().getProductId(),
            entity.getOptionValue() == null ? null : entity.getOptionValue().getOptionValueId(),
            entity.getUrl(),
            entity.getAltText(),
            entity.getPosition(),
            entity.getIsPrimary(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
