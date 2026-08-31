package app.ecommerce.catalog.api.event;

import java.time.Instant;
import java.util.UUID;

public record CategoryDeactivatedEvent(UUID categoryId, Instant occurredAt) {
}