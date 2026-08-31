package app.ecommerce.product.impl.listener;

import app.ecommerce.catalog.api.event.CategoryDeactivatedEvent;
import app.ecommerce.product.impl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cascades a category soft-delete down to its products.
 *
 * <p>Runs synchronously inside the category deactivation transaction, so the
 * product updates commit or roll back together with the category.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCategoryCascadeListener {

    private final ProductRepository productRepository;

    @EventListener
    public void onCategoryDeactivated(final CategoryDeactivatedEvent event) {
        final var deactivatedProducts = productRepository.deactivateAllByCategoryId(
            event.categoryId(),
            event.occurredAt()
        );
        log.info(
            "Products deactivated by category cascade: categoryId={}, cascadedProducts={}",
            event.categoryId(),
            deactivatedProducts
        );
    }
}
