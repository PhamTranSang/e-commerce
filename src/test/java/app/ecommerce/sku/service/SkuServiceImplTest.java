package app.ecommerce.sku.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.api.exceptions.SkuAlreadyExistsException;
import app.ecommerce.sku.api.exceptions.SkuNotFoundException;
import app.ecommerce.sku.impl.entity.SkuEntity;
import app.ecommerce.sku.impl.mapper.SkuMapper;
import app.ecommerce.sku.impl.repository.SkuRepository;
import app.ecommerce.sku.impl.service.SkuServiceImpl;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class SkuServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-31T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final SkuRepository repository = mock(SkuRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final SkuServiceImpl service =
        new SkuServiceImpl(
            repository,
            productRepository,
            new SkuMapper(),
            CLOCK,
            new DatabaseConstraintInspector()
        );

    @Test
    void createsSkuWithNormalizedCodeAndServerManagedFields() {
        final var productId = UUID.randomUUID();
        final var skuId = UUID.randomUUID();
        final var product = product(productId);
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product));
        when(repository.existsBySkuCode("SKU-001")).thenReturn(false);
        when(repository.saveAndFlush(any(SkuEntity.class))).thenAnswer(invocation -> {
            final SkuEntity entity = invocation.getArgument(0);
            entity.setSkuId(skuId);
            return entity;
        });

        final var response = service.createSku(new CreateSkuRequest(
            productId, "  SKU-001  ", 500, new BigDecimal("19.99"), "USD"));

        assertThat(response.skuId()).isEqualTo(skuId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.skuCode()).isEqualTo("SKU-001");
        assertThat(response.weightGrams()).isEqualTo(500);
        assertThat(response.amount()).isEqualByComparingTo("19.99");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsSkuWhenProductIsMissingOrInactive() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSku(new CreateSkuRequest(
            productId, "SKU-001", 500, new BigDecimal("19.99"), "USD")))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("Product '%s' was not found".formatted(productId));

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void rejectsSkuWhenCodeAlreadyExists() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(repository.existsBySkuCode("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> service.createSku(new CreateSkuRequest(
            productId, " SKU-001 ", 500, new BigDecimal("19.99"), "USD")))
            .isInstanceOf(SkuAlreadyExistsException.class)
            .hasMessage("SKU 'SKU-001' already exists");

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void translatesConcurrentUniqueConstraintViolationToBusinessConflict() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(repository.existsBySkuCode("SKU-001")).thenReturn(false);
        final var constraintViolation = new ConstraintViolationException(
            "duplicate sku code",
            new SQLException(),
            "uq_sku_code"
        );
        final var databaseException =
            new DataIntegrityViolationException("duplicate sku code", constraintViolation);
        when(repository.saveAndFlush(any(SkuEntity.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.createSku(new CreateSkuRequest(
            productId, "SKU-001", 500, new BigDecimal("19.99"), "USD")))
            .isInstanceOf(SkuAlreadyExistsException.class)
            .hasMessage("SKU 'SKU-001' already exists")
            .hasCause(databaseException);
    }

    @Test
    void getsActiveSkuDetail() {
        final var skuId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = sku(skuId, productId, "SKU-001", createdAt, NOW);
        when(repository.findBySkuIdAndIsActiveTrue(skuId)).thenReturn(Optional.of(entity));

        final var response = service.getSku(skuId);

        assertThat(response.skuId()).isEqualTo(skuId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.skuCode()).isEqualTo("SKU-001");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsSkuDetailWhenSkuIsMissingOrInactive() {
        final var skuId = UUID.randomUUID();
        when(repository.findBySkuIdAndIsActiveTrue(skuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSku(skuId))
            .isInstanceOf(SkuNotFoundException.class)
            .hasMessage("SKU '%s' was not found".formatted(skuId));
    }

    @Test
    void getsActiveSkusUsingOneBasedPageAndStableSort() {
        final var productId = UUID.randomUUID();
        final var first = sku(
            UUID.randomUUID(), productId, "SKU-001",
            Instant.parse("2026-08-16T10:00:00Z"), NOW);
        final var second = sku(
            UUID.randomUUID(), productId, "SKU-002",
            Instant.parse("2026-08-15T10:00:00Z"), NOW);
        final var sort = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "skuId"));
        final var pageable = PageRequest.of(1, 2, sort);
        when(repository.findAllByProduct_ProductIdAndIsActiveTrue(productId, pageable))
            .thenReturn(new PageImpl<>(List.of(first, second), pageable, 4));

        final var response = service.getSkus(productId, 2, 2);

        assertThat(response.content())
            .extracting(SkuResponse::skuCode)
            .containsExactly("SKU-001", "SKU-002");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(4);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasPrevious()).isTrue();
        verify(repository).findAllByProduct_ProductIdAndIsActiveTrue(productId, pageable);
    }

    @Test
    void updatesSkuAndTimestampWhilePreservingServerManagedFieldsAndCode() {
        final var skuId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = sku(skuId, productId, "SKU-001", createdAt, createdAt);
        when(repository.findBySkuIdAndIsActiveTrue(skuId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        final var response = service.updateSku(
            skuId, new UpdateSkuRequest(750, new BigDecimal("29.99"), "EUR"));

        assertThat(response.skuCode()).isEqualTo("SKU-001");
        assertThat(response.weightGrams()).isEqualTo(750);
        assertThat(response.amount()).isEqualByComparingTo("29.99");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void rejectsUpdateWhenSkuDoesNotExist() {
        final var skuId = UUID.randomUUID();
        when(repository.findBySkuIdAndIsActiveTrue(skuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSku(
            skuId, new UpdateSkuRequest(500, new BigDecimal("19.99"), "USD")))
            .isInstanceOf(SkuNotFoundException.class)
            .hasMessage("SKU '%s' was not found".formatted(skuId));

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void deactivatesSkuAndUpdatesTimestamp() {
        final var skuId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = sku(skuId, productId, "SKU-001", createdAt, createdAt);
        when(repository.findById(skuId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        service.deactivateSku(skuId);

        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void treatsAlreadyInactiveSkuAsSuccessfulDeactivation() {
        final var skuId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        final var updatedAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = sku(skuId, productId, "SKU-001", updatedAt, updatedAt);
        entity.setIsActive(false);
        when(repository.findById(skuId)).thenReturn(Optional.of(entity));

        service.deactivateSku(skuId);

        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void rejectsDeactivationWhenSkuDoesNotExist() {
        final var skuId = UUID.randomUUID();
        when(repository.findById(skuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateSku(skuId))
            .isInstanceOf(SkuNotFoundException.class)
            .hasMessage("SKU '%s' was not found".formatted(skuId));

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    private ProductEntity product(final UUID productId) {
        final var entity = new ProductEntity();
        entity.setProductId(productId);
        entity.setProductName("Laptop");
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private SkuEntity sku(
            final UUID skuId,
            final UUID productId,
            final String skuCode,
            final Instant createdAt,
            final Instant updatedAt) {
        final var entity = new SkuEntity();
        entity.setSkuId(skuId);
        entity.setProduct(product(productId));
        entity.setSkuCode(skuCode);
        entity.setWeightGrams(500);
        entity.setAmount(new BigDecimal("19.99"));
        entity.setCurrency("USD");
        entity.setIsActive(true);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }
}
