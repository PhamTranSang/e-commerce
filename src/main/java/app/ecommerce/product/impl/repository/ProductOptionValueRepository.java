package app.ecommerce.product.impl.repository;

import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValueEntity, UUID> {

    @Query("""
        SELECT v FROM ProductOptionValueEntity v
        WHERE v.option.optionId IN :optionIds ORDER BY v.position ASC
        """)
    List<ProductOptionValueEntity> findByOptionsOrderByPosition(
        @Param("optionIds") final List<UUID> optionIds
    );

    @Query("""
        SELECT COUNT(v) > 0 FROM ProductOptionValueEntity v
        WHERE v.optionValueId = :optionValueId AND v.option.product.productId = :productId
        """)
    boolean existsForProduct(
        @Param("optionValueId") final UUID optionValueId,
        @Param("productId") final UUID productId
    );
}