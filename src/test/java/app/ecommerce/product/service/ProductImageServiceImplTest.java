package app.ecommerce.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.ecommerce.product.api.dto.request.CreateProductImageRequest;
import app.ecommerce.product.api.dto.response.ProductImageResponse;
import app.ecommerce.product.api.exceptions.OptionValueNotInProductException;
import app.ecommerce.product.api.exceptions.ProductImageAlreadyHasPrimaryException;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.entity.ProductImageEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import app.ecommerce.product.impl.mapper.ProductImageMapper;
import app.ecommerce.product.impl.repository.ProductImageRepository;
import app.ecommerce.product.impl.repository.ProductOptionValueRepository;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.product.impl.service.ProductImageServiceImpl;
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

class ProductImageServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductImageRepository imageRepository = mock(ProductImageRepository.class);
    private final ProductOptionValueRepository optionValueRepository =
        mock(ProductOptionValueRepository.class);
    private final ProductImageServiceImpl service =
        new ProductImageServiceImpl(
            productRepository,
            imageRepository,
            optionValueRepository,
            new ProductImageMapper(),
            CLOCK,
            new DatabaseConstraintInspector()
        );

    @Test
    void addsGeneralImageWithServerManagedFields() {
        final var productId = UUID.randomUUID();
        final var imageId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(imageRepository.countByProduct(productId)).thenReturn(0L);
        when(imageRepository.saveAndFlush(any(ProductImageEntity.class))).thenAnswer(invocation -> {
            final ProductImageEntity image = invocation.getArgument(0);
            image.setImageId(imageId);
            return image;
        });

        final var response = service.addImage(productId, new CreateProductImageRequest(
            "  https://cdn/x.jpg  ", "  Front view  ", null, null));

        assertThat(response.imageId()).isEqualTo(imageId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.optionValueId()).isNull();
        assertThat(response.url()).isEqualTo("https://cdn/x.jpg");
        assertThat(response.altText()).isEqualTo("Front view");
        assertThat(response.position()).isEqualTo(0);
        assertThat(response.isPrimary()).isFalse();
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void addsImageTiedToOptionValue() {
        final var productId = UUID.randomUUID();
        final var optionValueId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(optionValueRepository.existsForProduct(optionValueId, productId)).thenReturn(true);
        when(optionValueRepository.getReferenceById(optionValueId))
            .thenReturn(optionValue(optionValueId));
        when(imageRepository.countByProduct(productId)).thenReturn(1L);
        when(imageRepository.saveAndFlush(any(ProductImageEntity.class)))
            .thenAnswer(invocation -> {
                final ProductImageEntity image = invocation.getArgument(0);
                image.setImageId(UUID.randomUUID());
                return image;
            });

        final var response = service.addImage(productId, new CreateProductImageRequest(
            "https://cdn/blue.jpg", null, optionValueId, false));

        assertThat(response.optionValueId()).isEqualTo(optionValueId);
        assertThat(response.position()).isEqualTo(1);
    }

    @Test
    void rejectsAddWhenProductMissingOrInactive() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addImage(productId, new CreateProductImageRequest(
            "https://cdn/x.jpg", null, null, null)))
            .isInstanceOf(ProductNotFoundException.class);

        verify(imageRepository, never()).saveAndFlush(any(ProductImageEntity.class));
    }

    @Test
    void rejectsAddWhenOptionValueNotInProduct() {
        final var productId = UUID.randomUUID();
        final var optionValueId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(optionValueRepository.existsForProduct(optionValueId, productId)).thenReturn(false);

        assertThatThrownBy(() -> service.addImage(productId, new CreateProductImageRequest(
            "https://cdn/x.jpg", null, optionValueId, null)))
            .isInstanceOf(OptionValueNotInProductException.class);

        verify(imageRepository, never()).saveAndFlush(any(ProductImageEntity.class));
    }

    @Test
    void rejectsAddPrimaryWhenPrimaryAlreadyExists() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(imageRepository.existsPrimaryForProduct(productId)).thenReturn(true);

        assertThatThrownBy(() -> service.addImage(productId, new CreateProductImageRequest(
            "https://cdn/x.jpg", null, null, true)))
            .isInstanceOf(ProductImageAlreadyHasPrimaryException.class);

        verify(imageRepository, never()).saveAndFlush(any(ProductImageEntity.class));
    }

    @Test
    void translatesConcurrentPrimaryViolationToConflict() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product(productId)));
        when(imageRepository.existsPrimaryForProduct(productId)).thenReturn(false);
        when(imageRepository.countByProduct(productId)).thenReturn(0L);
        final var constraintViolation = new ConstraintViolationException(
            "duplicate primary", new SQLException(), "uq_product_image_primary");
        final var databaseException =
            new DataIntegrityViolationException("duplicate primary", constraintViolation);
        when(imageRepository.saveAndFlush(any(ProductImageEntity.class)))
            .thenThrow(databaseException);

        assertThatThrownBy(() -> service.addImage(productId, new CreateProductImageRequest(
            "https://cdn/x.jpg", null, null, true)))
            .isInstanceOf(ProductImageAlreadyHasPrimaryException.class)
            .hasCause(databaseException);
    }

    @Test
    void getsProductImagesOrdered() {
        final var productId = UUID.randomUUID();
        final var product = product(productId);
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.of(product));
        when(imageRepository.findByProductOrderByPosition(productId))
            .thenReturn(List.of(
                image(product, "https://cdn/1.jpg", 0),
                image(product, "https://cdn/2.jpg", 1)));

        final var images = service.getProductImages(productId);

        assertThat(images).extracting(ProductImageResponse::url)
            .containsExactly("https://cdn/1.jpg", "https://cdn/2.jpg");
    }

    @Test
    void getProductImagesRejectsWhenProductMissing() {
        final var productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsActiveTrue(productId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProductImages(productId))
            .isInstanceOf(ProductNotFoundException.class);
    }

    private ProductEntity product(final UUID productId) {
        final var entity = new ProductEntity();
        entity.setProductId(productId);
        entity.setProductName("iPhone 15 Pro");
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private ProductOptionValueEntity optionValue(final UUID optionValueId) {
        final var entity = new ProductOptionValueEntity();
        entity.setOptionValueId(optionValueId);
        entity.setValue("Blue");
        entity.setPosition(0);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private ProductImageEntity image(
            final ProductEntity product, final String url, final int position) {
        final var entity = new ProductImageEntity();
        entity.setImageId(UUID.randomUUID());
        entity.setProduct(product);
        entity.setUrl(url);
        entity.setPosition(position);
        entity.setIsPrimary(false);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }
}
