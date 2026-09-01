package app.ecommerce.sku.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.ecommerce.product.api.event.ProductDeactivatedEvent;
import app.ecommerce.sku.impl.listener.SkuProductCascadeListener;
import app.ecommerce.sku.impl.repository.SkuRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkuProductCascadeListenerTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    private final SkuRepository skuRepository = mock(SkuRepository.class);
    private final SkuProductCascadeListener listener =
        new SkuProductCascadeListener(skuRepository);

    @Test
    void cascadesSoftDeleteToSkusOfDeactivatedProduct() {
        final var productId = UUID.randomUUID();

        listener.onProductDeactivated(new ProductDeactivatedEvent(productId, NOW));

        verify(skuRepository).deactivateAllByProductId(productId, NOW);
    }
}
