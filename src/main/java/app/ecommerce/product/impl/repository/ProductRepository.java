package app.ecommerce.product.impl.repository;

import app.ecommerce.product.impl.entity.ProductEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsByCategory_CategoryIdAndProductNameIgnoreCase(
        final UUID categoryId,
        final String productName
    );

    boolean existsByCategory_CategoryIdAndProductNameIgnoreCaseAndProductIdNot(
        final UUID categoryId,
        final String productName,
        final UUID productId
    );

    Optional<ProductEntity> findByProductIdAndIsActiveTrue(final UUID productId);

    Page<ProductEntity> findAllByCategory_CategoryIdAndIsActiveTrue(
        final UUID categoryId,
        final Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE ProductEntity p
        SET p.isActive = false, p.updatedAt = :now
        WHERE p.category.categoryId = :categoryId AND p.isActive = true
        """)
    int deactivateAllByCategoryId(
        @Param("categoryId") final UUID categoryId,
        @Param("now") final Instant now
    );
}