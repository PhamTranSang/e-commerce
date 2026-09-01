package app.ecommerce.product.impl.repository;

import app.ecommerce.product.impl.entity.ProductEntity;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<ProductEntity> activeInCategory(final UUID categoryId) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("category").get("categoryId"), categoryId),
            cb.equal(root.get("isActive"), Boolean.TRUE)
        );
    }
}