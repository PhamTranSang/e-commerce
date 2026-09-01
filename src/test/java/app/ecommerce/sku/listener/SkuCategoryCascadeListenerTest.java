package app.ecommerce.sku.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.ecommerce.catalog.api.event.CategoryDeactivatedEvent;
import app.ecommerce.sku.impl.listener.SkuCategoryCascadeListener;
import app.ecommerce.sku.impl.repository.SkuRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkuCategoryCascadeListenerTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    private final SkuRepository skuRepository = mock(SkuRepository.class);
    private final SkuCategoryCascadeListener listener =
        new SkuCategoryCascadeListener(skuRepository);

    @Test
    void cascadesSoftDeleteToSkusOfDeactivatedCategory() {
        final var categoryId = UUID.randomUUID();

        listener.onCategoryDeactivated(new CategoryDeactivatedEvent(categoryId, NOW));

        verify(skuRepository).deactivateAllByCategoryId(categoryId, NOW);
    }
}
