package app.ecommerce.product.impl.repository;

import app.ecommerce.product.impl.entity.ProductOptionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductOptionRepository extends JpaRepository<ProductOptionEntity, UUID> {

    @Query("""
        SELECT o FROM ProductOptionEntity o
        WHERE o.product.productId = :productId ORDER BY o.position ASC
        """)
    List<ProductOptionEntity> findByProductOrderByPosition(@Param("productId") final UUID productId);

    @Query("""
        SELECT COUNT(o) > 0 FROM ProductOptionEntity o
        WHERE o.product.productId = :productId AND o.optionName = :optionName
        """)
    boolean existsByProductAndName(
        @Param("productId") final UUID productId,
        @Param("optionName") final String optionName
    );

    @Query("SELECT COUNT(o) FROM ProductOptionEntity o WHERE o.product.productId = :productId")
    long countByProduct(@Param("productId") final UUID productId);
}