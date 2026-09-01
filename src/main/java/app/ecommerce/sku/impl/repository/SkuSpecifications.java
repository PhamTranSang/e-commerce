package app.ecommerce.sku.impl.repository;

import app.ecommerce.sku.impl.entity.SkuEntity;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable query fragments for {@link SkuEntity}.
 * Combine with {@code Specification.and(...)} / {@code or(...)} for dynamic filtering.
 */
public final class SkuSpecifications {

    private SkuSpecifications() {
    }

    /** Active SKUs that belong to the given product. */
    public static Specification<SkuEntity> activeForProduct(final UUID productId) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("product").get("productId"), productId),
            cb.equal(root.get("isActive"), Boolean.TRUE)
        );
    }
}
