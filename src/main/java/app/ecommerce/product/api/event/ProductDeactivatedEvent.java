package app.ecommerce.product.api.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a product is soft-deleted so downstream modules can cascade
 * their own soft-delete without the product module depending on them.
 *
 * <p>Handled synchronously within the deactivation transaction.
 */
public record ProductDeactivatedEvent(UUID productId, Instant occurredAt) {
}
