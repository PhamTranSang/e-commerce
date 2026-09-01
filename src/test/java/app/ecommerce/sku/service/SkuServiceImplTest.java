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
import app.ecommerce.product.impl.entity.ProductOptionEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import app.ecommerce.product.impl.repository.ProductOptionRepository;
import app.ecommerce.product.impl.repository.ProductOptionValueRepository;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.api.exceptions.InvalidVariantCombinationException;
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

class SkuServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final SkuRepository repository = mock(SkuRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductOptionRepository optionRepository = mock(ProductOptionRepository.class);
    private final ProductOptionValueRepository optionValueRepository =
        mock(ProductOptionValueRepository.class);
    private final SkuServiceImpl service =
        new SkuServiceImpl(
            repository,
            productRepository,
            optionRepository,
            optionValueRepository,
            new SkuMapper(),
            CLOCK,
            new DatabaseConstraintInspector()
        );

    // A product with two options: Color (Blue/Natural) and Storage (256GB).
    private final UUID productId = UUID.randomUUID();
    private final ProductEntity product = product(productId);
    private final ProductOptionEntity colorOption = option(product, "Color");
    private final ProductOptionEntity storageOption = option(product, "Storage");
    private final ProductOptionValueEntity blue = value(colorOption);
    private final ProductOptionValueEntity natural = value(colorOption);
    private final ProductOptionValueEntity gb256 = value(storageOption);

    private void productHasTwoOptions() {
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product));
        when(optionRepository.findByProductOrderByPosition(productId))
            .thenReturn(List.of(colorOption, storageOption));
    }

    private CreateSkuRequest createRequest(final List<UUID> optionValueIds) {
        return new CreateSkuRequest(
            productId, "SKU-001", new BigDecimal("999.00"), "USD", 500, optionValueIds);
    }

    @Test
    void createsSkuWithValidCombination() {
        productHasTwoOptions();
        final var skuId = UUID.randomUUID();
        when(repository.existsBySkuCode("SKU-001")).thenReturn(false);
        when(optionValueRepository.findAllById(List.of(blue.getOptionValueId(), gb256.getOptionValueId())))
            .thenReturn(List.of(blue, gb256));
        when(repository.saveAndFlush(any(SkuEntity.class))).thenAnswer(invocation -> {
            final SkuEntity sku = invocation.getArgument(0);
            sku.setSkuId(skuId);
            return sku;
        });

        final var response = service.createSku(
            createRequest(List.of(blue.getOptionValueId(), gb256.getOptionValueId())));

        assertThat(response.skuId()).isEqualTo(skuId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.skuCode()).isEqualTo("SKU-001");
        assertThat(response.optionValueIds())
            .containsExactlyInAnyOrder(blue.getOptionValueId(), gb256.getOptionValueId());
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsCreateWhenProductMissing() {
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSku(createRequest(List.of())))
            .isInstanceOf(ProductNotFoundException.class);

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void rejectsCreateWhenSkuCodeAlreadyExists() {
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product));
        when(repository.existsBySkuCode("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> service.createSku(createRequest(List.of())))
            .isInstanceOf(SkuAlreadyExistsException.class);

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void rejectsCreateWhenCombinationMissesAnOption() {
        productHasTwoOptions();
        when(repository.existsBySkuCode("SKU-001")).thenReturn(false);
        when(optionValueRepository.findAllById(List.of(blue.getOptionValueId())))
            .thenReturn(List.of(blue));

        assertThatThrownBy(() -> service.createSku(createRequest(List.of(blue.getOptionValueId()))))
            .isInstanceOf(InvalidVariantCombinationException.class);

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void rejectsCreateWhenTwoValuesForSameOption() {
        productHasTwoOptions();
        when(repository.existsBySkuCode("SKU-001")).thenReturn(false);
        when(optionValueRepository.findAllById(
            List.of(blue.getOptionValueId(), natural.getOptionValueId())))
            .thenReturn(List.of(blue, natural));

        assertThatThrownBy(() -> service.createSku(
            createRequest(List.of(blue.getOptionValueId(), natural.getOptionValueId()))))
            .isInstanceOf(InvalidVariantCombinationException.class);

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void rejectsCreateWhenValueNotPartOfProduct() {
        productHasTwoOptions();
        final var foreignValue = value(option(product(UUID.randomUUID()), "Ram"));
        when(repository.existsBySkuCode("SKU-001")).thenReturn(false);
        when(optionValueRepository.findAllById(
            List.of(blue.getOptionValueId(), foreignValue.getOptionValueId())))
            .thenReturn(List.of(blue, foreignValue));

        assertThatThrownBy(() -> service.createSku(
            createRequest(List.of(blue.getOptionValueId(), foreignValue.getOptionValueId()))))
            .isInstanceOf(InvalidVariantCombinationException.class);

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void rejectsCreateWhenAnOptionValueDoesNotExist() {
        productHasTwoOptions();
        final var unknownId = UUID.randomUUID();
        when(repository.existsBySkuCode("SKU-001")).thenReturn(false);
        when(optionValueRepository.findAllById(List.of(blue.getOptionValueId(), unknownId)))
            .thenReturn(List.of(blue));

        assertThatThrownBy(() -> service.createSku(
            createRequest(List.of(blue.getOptionValueId(), unknownId))))
            .isInstanceOf(InvalidVariantCombinationException.class);

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void translatesConcurrentSkuCodeViolationToConflict() {
        productHasTwoOptions();
        when(repository.existsBySkuCode("SKU-001")).thenReturn(false);
        when(optionValueRepository.findAllById(List.of(blue.getOptionValueId(), gb256.getOptionValueId())))
            .thenReturn(List.of(blue, gb256));
        final var constraintViolation = new ConstraintViolationException(
            "duplicate sku code", new SQLException(), "uq_sku_code");
        final var databaseException =
            new DataIntegrityViolationException("duplicate sku code", constraintViolation);
        when(repository.saveAndFlush(any(SkuEntity.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.createSku(
            createRequest(List.of(blue.getOptionValueId(), gb256.getOptionValueId()))))
            .isInstanceOf(SkuAlreadyExistsException.class)
            .hasCause(databaseException);
    }

    @Test
    void getsActiveSkuDetail() {
        final var skuId = UUID.randomUUID();
        final var entity = sku(skuId, "SKU-001");
        when(repository.findBySkuIdAndIsActiveTrue(skuId)).thenReturn(Optional.of(entity));

        final var response = service.getSku(skuId);

        assertThat(response.skuId()).isEqualTo(skuId);
        assertThat(response.skuCode()).isEqualTo("SKU-001");
    }

    @Test
    void rejectsDetailWhenSkuMissing() {
        final var skuId = UUID.randomUUID();
        when(repository.findBySkuIdAndIsActiveTrue(skuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSku(skuId))
            .isInstanceOf(SkuNotFoundException.class);
    }

    @Test
    void updatesSkuPriceAndWeight() {
        final var skuId = UUID.randomUUID();
        final var entity = sku(skuId, "SKU-001");
        when(repository.findBySkuIdAndIsActiveTrue(skuId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        final var response = service.updateSku(
            skuId, new UpdateSkuRequest(new BigDecimal("1099.00"), "EUR", 600));

        assertThat(response.amount()).isEqualByComparingTo("1099.00");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.weightGrams()).isEqualTo(600);
        assertThat(response.skuCode()).isEqualTo("SKU-001");
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void deactivatesSku() {
        final var skuId = UUID.randomUUID();
        final var entity = sku(skuId, "SKU-001");
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
        final var entity = sku(skuId, "SKU-001");
        entity.setIsActive(false);
        when(repository.findById(skuId)).thenReturn(Optional.of(entity));

        service.deactivateSku(skuId);

        verify(repository, never()).saveAndFlush(any(SkuEntity.class));
    }

    @Test
    void rejectsDeactivationWhenSkuMissing() {
        final var skuId = UUID.randomUUID();
        when(repository.findById(skuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateSku(skuId))
            .isInstanceOf(SkuNotFoundException.class);
    }

    private ProductEntity product(final UUID id) {
        final var entity = new ProductEntity();
        entity.setProductId(id);
        entity.setProductName("iPhone 15 Pro");
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private ProductOptionEntity option(final ProductEntity product, final String name) {
        final var entity = new ProductOptionEntity();
        entity.setOptionId(UUID.randomUUID());
        entity.setProduct(product);
        entity.setOptionName(name);
        entity.setPosition(0);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private ProductOptionValueEntity value(final ProductOptionEntity option) {
        final var entity = new ProductOptionValueEntity();
        entity.setOptionValueId(UUID.randomUUID());
        entity.setOption(option);
        entity.setValue("v");
        entity.setPosition(0);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private SkuEntity sku(final UUID skuId, final String skuCode) {
        final var entity = new SkuEntity();
        entity.setSkuId(skuId);
        entity.setProduct(product);
        entity.setSkuCode(skuCode);
        entity.setAmount(new BigDecimal("999.00"));
        entity.setCurrency("USD");
        entity.setWeightGrams(500);
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }
}
