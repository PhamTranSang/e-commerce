package app.ecommerce.catalog.impl.mapper;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryResponse;
import app.ecommerce.catalog.impl.entity.CategoryEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryEntity toNewEntity(
        final CreateCategoryRequest request,
        final CategoryEntity parent,
        final Instant now
    ) {
        final var entity = new CategoryEntity();
        entity.setParent(parent);
        entity.setCategoryName(request.categoryName());
        entity.setIsActive(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public void rename(
        final CategoryEntity entity,
        final RenameCategoryRequest request,
        final Instant updatedAt
    ) {
        entity.setCategoryName(request.categoryName());
        entity.setUpdatedAt(updatedAt);
    }

    public void deactivate(final CategoryEntity entity, final Instant updatedAt) {
        entity.setIsActive(false);
        entity.setUpdatedAt(updatedAt);
    }

    public CategoryResponse toResponse(final CategoryEntity entity) {
        return new CategoryResponse(
            entity.getCategoryId(),
            entity.getParent() == null ? null : entity.getParent().getCategoryId(),
            entity.getCategoryName(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
