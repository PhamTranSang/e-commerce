package app.ecommerce.product.impl.mapper;

import app.ecommerce.product.api.dto.response.ProductOptionResponse;
import app.ecommerce.product.api.dto.response.ProductOptionValueResponse;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.entity.ProductOptionEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductOptionMapper {

    public ProductOptionEntity toNewOption(
        final ProductEntity product,
        final String optionName,
        final int position,
        final Instant now
    ) {
        final var entity = new ProductOptionEntity();
        entity.setProduct(product);
        entity.setOptionName(optionName);
        entity.setPosition(position);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public ProductOptionValueEntity toNewValue(
        final ProductOptionEntity option,
        final String value,
        final int position,
        final Instant now
    ) {
        final var entity = new ProductOptionValueEntity();
        entity.setOption(option);
        entity.setValue(value);
        entity.setPosition(position);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public ProductOptionResponse toResponse(
        final ProductOptionEntity option,
        final List<ProductOptionValueEntity> values
    ) {
        final var valueResponses = values.stream()
            .sorted(Comparator.comparing(ProductOptionValueEntity::getPosition))
            .map(this::toValueResponse)
            .toList();
        return new ProductOptionResponse(
            option.getOptionId(),
            option.getOptionName(),
            option.getPosition(),
            valueResponses
        );
    }

    public ProductOptionValueResponse toValueResponse(final ProductOptionValueEntity entity) {
        return new ProductOptionValueResponse(
            entity.getOptionValueId(),
            entity.getValue(),
            entity.getPosition()
        );
    }
}
