package app.ecommerce.product.impl.repository;

import app.ecommerce.product.impl.entity.ProductEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
        extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByProductIdAndIsActiveTrue(final UUID productId);

    @Modifying
    @Query("""
        UPDATE ProductEntity p SET p.isActive = false, p.updatedAt = :now
        WHERE p.category.categoryId = :categoryId AND p.isActive = true
        """)
    int deactivateAllByCategoryId(
        @Param("categoryId") final UUID categoryId,
        @Param("now") final Instant now
    );

    /**
     * Whether a product with the same name already exists in the given category, optionally
     * excluding one product (used on update to skip the row being changed).
     *
     * <p>Uses {@code LOWER(...)} to match the case-insensitive unique index {@code uq_product_name}.
     */
    @Query("""
        SELECT COUNT(p) > 0 FROM ProductEntity p
        WHERE p.category.categoryId = :categoryId
          AND LOWER(p.productName) = LOWER(:productName)
          AND (:excludeProductId IS NULL OR p.productId <> :excludeProductId)
        """)
    boolean existsByNameInCategory(
        @Param("categoryId") final UUID categoryId,
        @Param("productName") final String productName,
        @Param("excludeProductId") final UUID excludeProductId
    );
}
