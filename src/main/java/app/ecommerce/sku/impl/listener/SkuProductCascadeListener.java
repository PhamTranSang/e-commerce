package app.ecommerce.sku.impl.listener;

import app.ecommerce.product.api.event.ProductDeactivatedEvent;
import app.ecommerce.sku.impl.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cascades a product soft-delete down to its SKUs.
 *
 * <p>Runs synchronously inside the product deactivation transaction, so the SKU updates
 * commit or roll back together with the product.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkuProductCascadeListener {

    private final SkuRepository skuRepository;

    @EventListener
    public void onProductDeactivated(final ProductDeactivatedEvent event) {
        final var deactivatedSkus = skuRepository.deactivateAllByProductId(
            event.productId(), event.occurredAt());
        log.info(
            "SKUs deactivated by product cascade: productId={}, cascadedSkus={}",
            event.productId(), deactivatedSkus);
    }
}
