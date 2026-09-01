package app.ecommerce.sku.impl.repository;

import app.ecommerce.sku.impl.entity.SkuEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkuRepository
        extends JpaRepository<SkuEntity, UUID>, JpaSpecificationExecutor<SkuEntity> {

    Optional<SkuEntity> findBySkuIdAndIsActiveTrue(final UUID skuId);

    boolean existsBySkuCode(final String skuCode);

    @Modifying
    @Query("""
        UPDATE SkuEntity s SET s.isActive = false, s.updatedAt = :now
        WHERE s.product.productId = :productId AND s.isActive = true
        """)
    int deactivateAllByProductId(
        @Param("productId") final UUID productId,
        @Param("now") final Instant now
    );

    @Modifying
    @Query("""
        UPDATE SkuEntity s SET s.isActive = false, s.updatedAt = :now
        WHERE s.product.category.categoryId = :categoryId AND s.isActive = true
        """)
    int deactivateAllByCategoryId(
        @Param("categoryId") final UUID categoryId,
        @Param("now") final Instant now
    );
}
