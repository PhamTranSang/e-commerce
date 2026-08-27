package app.ecommerce.catalog.impl.repository;

import app.ecommerce.catalog.impl.entity.CategoryEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    boolean existsByCategoryNameIgnoreCase(final String categoryName);

    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(final String categoryName, final UUID categoryId);

    Optional<CategoryEntity> findByCategoryIdAndIsActiveTrue(final UUID categoryId);

    Page<CategoryEntity> findAllByIsActiveTrue(final Pageable pageable);
}
