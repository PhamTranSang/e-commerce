package app.ecommerce.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.ecommerce.product.api.dto.request.CreateProductOptionRequest;
import app.ecommerce.product.api.dto.response.ProductOptionResponse;
import app.ecommerce.product.api.dto.response.ProductOptionValueResponse;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.api.exceptions.ProductOptionAlreadyExistsException;
import app.ecommerce.product.api.exceptions.ProductOptionValueAlreadyExistsException;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.entity.ProductOptionEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import app.ecommerce.product.impl.mapper.ProductOptionMapper;
import app.ecommerce.product.impl.repository.ProductOptionRepository;
import app.ecommerce.product.impl.repository.ProductOptionValueRepository;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.product.impl.service.ProductOptionServiceImpl;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
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

class ProductOptionServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductOptionRepository optionRepository = mock(ProductOptionRepository.class);
    private final ProductOptionValueRepository valueRepository =
        mock(ProductOptionValueRepository.class);
    private final ProductOptionServiceImpl service =
        new ProductOptionServiceImpl(
            productRepository,
            optionRepository,
            valueRepository,
            new ProductOptionMapper(),
            CLOCK,
            new DatabaseConstraintInspector()
        );

    @Test
    void addsOptionWithNormalizedNameAndValues() {
        final var productId = UUID.randomUUID();
        final var optionId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(optionRepository.existsByProductAndName(productId, "Color"))
            .thenReturn(false);
        when(optionRepository.countByProduct(productId)).thenReturn(0L);
        when(optionRepository.saveAndFlush(any(ProductOptionEntity.class))).thenAnswer(invocation -> {
            final ProductOptionEntity option = invocation.getArgument(0);
            option.setOptionId(optionId);
            return option;
        });

        final var response = service.addOption(
            productId, new CreateProductOptionRequest("  Color  ", List.of("  Blue  ", "Natural")));

        assertThat(response.optionId()).isEqualTo(optionId);
        assertThat(response.optionName()).isEqualTo("Color");
        assertThat(response.position()).isEqualTo(0);
        assertThat(response.values()).extracting(ProductOptionValueResponse::value)
            .containsExactly("Blue", "Natural");
        assertThat(response.values()).extracting(ProductOptionValueResponse::position)
            .containsExactly(0, 1);
    }

    @Test
    void rejectsAddWhenProductMissingOrInactive() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addOption(
            productId, new CreateProductOptionRequest("Color", List.of("Blue"))))
            .isInstanceOf(ProductNotFoundException.class);

        verify(optionRepository, never()).saveAndFlush(any(ProductOptionEntity.class));
    }

    @Test
    void rejectsAddWhenOptionNameAlreadyExists() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(optionRepository.existsByProductAndName(productId, "Color"))
            .thenReturn(true);

        assertThatThrownBy(() -> service.addOption(
            productId, new CreateProductOptionRequest("Color", List.of("Blue"))))
            .isInstanceOf(ProductOptionAlreadyExistsException.class);

        verify(optionRepository, never()).saveAndFlush(any(ProductOptionEntity.class));
    }

    @Test
    void rejectsAddWhenValuesContainDuplicate() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(optionRepository.existsByProductAndName(productId, "Color"))
            .thenReturn(false);

        assertThatThrownBy(() -> service.addOption(
            productId, new CreateProductOptionRequest("Color", List.of("Blue", " Blue "))))
            .isInstanceOf(ProductOptionValueAlreadyExistsException.class);

        verify(optionRepository, never()).saveAndFlush(any(ProductOptionEntity.class));
    }

    @Test
    void translatesConcurrentOptionNameViolationToConflict() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(optionRepository.existsByProductAndName(productId, "Color"))
            .thenReturn(false);
        when(optionRepository.countByProduct(productId)).thenReturn(0L);
        final var constraintViolation = new ConstraintViolationException(
            "duplicate option", new SQLException(), "uq_option_name_per_product");
        final var databaseException =
            new DataIntegrityViolationException("duplicate option", constraintViolation);
        when(optionRepository.saveAndFlush(any(ProductOptionEntity.class)))
            .thenThrow(databaseException);

        assertThatThrownBy(() -> service.addOption(
            productId, new CreateProductOptionRequest("Color", List.of("Blue"))))
            .isInstanceOf(ProductOptionAlreadyExistsException.class)
            .hasCause(databaseException);
    }

    @Test
    void getsProductOptionsWithTheirValues() {
        final var productId = UUID.randomUUID();
        final var product = product(productId);
        final var colorOption = option(UUID.randomUUID(), product, "Color", 0);
        final var storageOption = option(UUID.randomUUID(), product, "Storage", 1);
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product));
        when(optionRepository.findByProductOrderByPosition(productId))
            .thenReturn(List.of(colorOption, storageOption));
        when(valueRepository.findByOptionsOrderByPosition(
            List.of(colorOption.getOptionId(), storageOption.getOptionId())))
            .thenReturn(List.of(
                value(colorOption, "Blue", 0),
                value(colorOption, "Natural", 1),
                value(storageOption, "256GB", 0)));

        final var options = service.getProductOptions(productId);

        assertThat(options).extracting(ProductOptionResponse::optionName)
            .containsExactly("Color", "Storage");
        assertThat(options.get(0).values()).extracting(ProductOptionValueResponse::value)
            .containsExactly("Blue", "Natural");
        assertThat(options.get(1).values()).extracting(ProductOptionValueResponse::value)
            .containsExactly("256GB");
    }

    @Test
    void getProductOptionsRejectsWhenProductMissing() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProductOptions(productId))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getProductOptionsReturnsEmptyWhenNoOptions() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(optionRepository.findByProductOrderByPosition(productId))
            .thenReturn(List.of());

        assertThat(service.getProductOptions(productId)).isEmpty();
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

    private ProductOptionEntity option(
            final UUID optionId, final ProductEntity product, final String name, final int position) {
        final var entity = new ProductOptionEntity();
        entity.setOptionId(optionId);
        entity.setProduct(product);
        entity.setOptionName(name);
        entity.setPosition(position);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private ProductOptionValueEntity value(
            final ProductOptionEntity option, final String value, final int position) {
        final var entity = new ProductOptionValueEntity();
        entity.setOptionValueId(UUID.randomUUID());
        entity.setOption(option);
        entity.setValue(value);
        entity.setPosition(position);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }
}