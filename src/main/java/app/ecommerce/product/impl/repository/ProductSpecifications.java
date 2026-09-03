package app.ecommerce.product.impl.repository;

import app.ecommerce.product.impl.entity.ProductEntity;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable query fragments for {@link ProductEntity}.
 * Combine with {@code Specification.and(...)} / {@code or(...)} for dynamic filtering.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /**
     * Active products, optionally narrowed to a category. A {@code null} category means
     * "all active products" (not an empty result).
     */
    public static Specification<ProductEntity> active(final UUID categoryId) {
        return (root, query, cb) -> {
            final var isActive = cb.equal(root.get("isActive"), Boolean.TRUE);
            if (categoryId == null) {
                return isActive;
            }
            return cb.and(isActive, cb.equal(root.get("category").get("categoryId"), categoryId));
        };
    }
}
