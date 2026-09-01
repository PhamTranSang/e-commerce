package app.ecommerce.brand.impl.mapper;

import app.ecommerce.brand.api.dto.request.CreateBrandRequest;
import app.ecommerce.brand.api.dto.request.RenameBrandRequest;
import app.ecommerce.brand.api.dto.response.BrandResponse;
import app.ecommerce.brand.impl.entity.BrandEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public BrandEntity toNewEntity(final CreateBrandRequest request, final Instant now) {
        final var entity = new BrandEntity();
        entity.setBrandName(request.brandName());
        entity.setIsActive(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public void rename(
        final BrandEntity entity,
        final RenameBrandRequest request,
        final Instant updatedAt
    ) {
        entity.setBrandName(request.brandName());
        entity.setUpdatedAt(updatedAt);
    }

    public void deactivate(final BrandEntity entity, final Instant updatedAt) {
        entity.setIsActive(false);
        entity.setUpdatedAt(updatedAt);
    }

    public BrandResponse toResponse(final BrandEntity entity) {
        return new BrandResponse(
            entity.getBrandId(),
            entity.getBrandName(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
