package app.ecommerce.product.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.ecommerce.catalog.api.event.CategoryDeactivatedEvent;
import app.ecommerce.product.impl.listener.ProductCategoryCascadeListener;
import app.ecommerce.product.impl.repository.ProductRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductCategoryCascadeListenerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductCategoryCascadeListener listener =
        new ProductCategoryCascadeListener(productRepository);

    @Test
    void cascadesSoftDeleteToProductsOfDeactivatedCategory() {
        final var categoryId = UUID.randomUUID();

        listener.onCategoryDeactivated(new CategoryDeactivatedEvent(categoryId, NOW));

        verify(productRepository).deactivateAllByCategoryId(categoryId, NOW);
    }
}
