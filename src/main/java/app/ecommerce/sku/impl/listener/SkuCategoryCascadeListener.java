package app.ecommerce.sku.impl.listener;

import app.ecommerce.catalog.api.event.CategoryDeactivatedEvent;
import app.ecommerce.sku.impl.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cascades a category soft-delete down to the SKUs of its products.
 *
 * <p>Runs synchronously inside the category deactivation transaction, alongside
 * the product cascade, so category, products and SKUs commit or roll back
 * together. Independent of the product listener: both filter by the event's
 * categoryId, so ordering between them does not matter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkuCategoryCascadeListener {

    private final SkuRepository skuRepository;

    @EventListener
    public void onCategoryDeactivated(final CategoryDeactivatedEvent event) {
        final var deactivatedSkus = skuRepository.deactivateAllByCategoryId(
            event.categoryId(),
            event.occurredAt()
        );
        log.info(
            "SKUs deactivated by category cascade: categoryId={}, cascadedSkus={}",
            event.categoryId(),
            deactivatedSkus
        );
    }
}
