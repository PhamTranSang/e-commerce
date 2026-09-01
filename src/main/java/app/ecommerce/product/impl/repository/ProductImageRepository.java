package app.ecommerce.product.impl.repository;

import app.ecommerce.product.impl.entity.ProductImageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, UUID> {

    @Query("""
        SELECT i FROM ProductImageEntity i
        WHERE i.product.productId = :productId ORDER BY i.position ASC
        """)
    List<ProductImageEntity> findByProductOrderByPosition(@Param("productId") final UUID productId);

    @Query("SELECT COUNT(i) FROM ProductImageEntity i WHERE i.product.productId = :productId")
    long countByProduct(@Param("productId") final UUID productId);

    @Query("""
        SELECT COUNT(i) > 0 FROM ProductImageEntity i
        WHERE i.product.productId = :productId AND i.isPrimary = true
        """)
    boolean existsPrimaryForProduct(@Param("productId") final UUID productId);
}
