package app.ecommerce.catalog.api.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a category is soft-deleted so downstream modules can cascade
 * their own soft-delete without the catalog module depending on them.
 *
 * <p>Handled synchronously within the deactivation transaction.
 */
public record CategoryDeactivatedEvent(UUID categoryId, Instant occurredAt) {
}
