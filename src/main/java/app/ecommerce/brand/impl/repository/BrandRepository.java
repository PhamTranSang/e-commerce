package app.ecommerce.brand.impl.repository;

import app.ecommerce.brand.impl.entity.BrandEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BrandRepository extends JpaRepository<BrandEntity, UUID> {

    Optional<BrandEntity> findByBrandIdAndIsActiveTrue(final UUID brandId);

    Page<BrandEntity> findAllByIsActiveTrue(final Pageable pageable);

    /**
     * Whether a brand with the same name already exists, optionally excluding one brand
     * (used on rename to skip the row being renamed).
     *
     * <p>Uses {@code LOWER(...)} to match the case-insensitive unique index {@code uq_brand_name}.
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM BrandEntity b
        WHERE LOWER(b.brandName) = LOWER(:brandName)
          AND (:excludeBrandId IS NULL OR b.brandId <> :excludeBrandId)
        """)
    boolean existsByNameIgnoringCase(
        @Param("brandName") final String brandName,
        @Param("excludeBrandId") final UUID excludeBrandId
    );
}
