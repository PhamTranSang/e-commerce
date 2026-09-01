package app.ecommerce.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.ecommerce.brand.api.exceptions.BrandNotFoundException;
import app.ecommerce.brand.impl.entity.BrandEntity;
import app.ecommerce.brand.impl.repository.BrandRepository;
import app.ecommerce.catalog.api.exceptions.CategoryNotFoundException;
import app.ecommerce.catalog.impl.entity.CategoryEntity;
import app.ecommerce.catalog.impl.repository.CategoryRepository;
import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.product.api.dto.request.UpdateProductRequest;
import app.ecommerce.product.api.event.ProductDeactivatedEvent;
import app.ecommerce.product.api.exceptions.LeafCategoryRequiredException;
import app.ecommerce.product.api.exceptions.ProductAlreadyExistsException;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.mapper.ProductMapper;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.product.impl.service.ProductServiceImpl;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

class ProductServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ProductRepository repository = mock(ProductRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final BrandRepository brandRepository = mock(BrandRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ProductServiceImpl service =
        new ProductServiceImpl(
            repository,
            categoryRepository,
            brandRepository,
            new ProductMapper(),
            CLOCK,
            new DatabaseConstraintInspector(),
            eventPublisher
        );

    @Test
    void createsProductWithNormalizedNameAndServerManagedFields() {
        final var categoryId = UUID.randomUUID();
        final var brandId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(category(categoryId)));
        when(categoryRepository.hasChildren(categoryId)).thenReturn(false);
        when(brandRepository.findByBrandIdAndIsActiveTrue(brandId))
            .thenReturn(Optional.of(brand(brandId)));
        when(repository.existsByNameInCategory(categoryId, "Laptop", null)).thenReturn(false);
        when(repository.saveAndFlush(any(ProductEntity.class))).thenAnswer(invocation -> {
            final ProductEntity entity = invocation.getArgument(0);
            entity.setProductId(productId);
            return entity;
        });

        final var response = service.createProduct(new CreateProductRequest(
            categoryId, brandId, "  Laptop  ", "  A portable computer  "));

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.brandId()).isEqualTo(brandId);
        assertThat(response.productName()).isEqualTo("Laptop");
        assertThat(response.productDescription()).isEqualTo("  A portable computer  ");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsProductWhenCategoryMissingOrInactive() {
        final var categoryId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProduct(new CreateProductRequest(
            categoryId, UUID.randomUUID(), "Laptop", null)))
            .isInstanceOf(CategoryNotFoundException.class);

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void rejectsProductWhenCategoryIsNotLeaf() {
        final var categoryId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(category(categoryId)));
        when(categoryRepository.hasChildren(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> service.createProduct(new CreateProductRequest(
            categoryId, UUID.randomUUID(), "Laptop", null)))
            .isInstanceOf(LeafCategoryRequiredException.class);

        verify(brandRepository, never()).findByBrandIdAndIsActiveTrue(any());
        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void rejectsProductWhenBrandMissingOrInactive() {
        final var categoryId = UUID.randomUUID();
        final var brandId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(category(categoryId)));
        when(categoryRepository.hasChildren(categoryId)).thenReturn(false);
        when(brandRepository.findByBrandIdAndIsActiveTrue(brandId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProduct(new CreateProductRequest(
            categoryId, brandId, "Laptop", null)))
            .isInstanceOf(BrandNotFoundException.class);

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void rejectsProductWhenNameExistsInCategory() {
        final var categoryId = UUID.randomUUID();
        final var brandId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(category(categoryId)));
        when(categoryRepository.hasChildren(categoryId)).thenReturn(false);
        when(brandRepository.findByBrandIdAndIsActiveTrue(brandId))
            .thenReturn(Optional.of(brand(brandId)));
        when(repository.existsByNameInCategory(categoryId, "Laptop", null)).thenReturn(true);

        assertThatThrownBy(() -> service.createProduct(new CreateProductRequest(
            categoryId, brandId, " Laptop ", null)))
            .isInstanceOf(ProductAlreadyExistsException.class);

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void translatesConcurrentUniqueConstraintViolationToConflict() {
        final var categoryId = UUID.randomUUID();
        final var brandId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(category(categoryId)));
        when(categoryRepository.hasChildren(categoryId)).thenReturn(false);
        when(brandRepository.findByBrandIdAndIsActiveTrue(brandId))
            .thenReturn(Optional.of(brand(brandId)));
        when(repository.existsByNameInCategory(categoryId, "Laptop", null)).thenReturn(false);
        final var constraintViolation = new ConstraintViolationException(
            "duplicate product name", new SQLException(), "uq_product_name");
        final var databaseException =
            new DataIntegrityViolationException("duplicate product name", constraintViolation);
        when(repository.saveAndFlush(any(ProductEntity.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.createProduct(new CreateProductRequest(
            categoryId, brandId, "Laptop", null)))
            .isInstanceOf(ProductAlreadyExistsException.class)
            .hasCause(databaseException);
    }

    @Test
    void getsActiveProductDetail() {
        final var productId = UUID.randomUUID();
        final var entity = product(productId, category(UUID.randomUUID()), brand(UUID.randomUUID()),
            "Laptop", "desc");
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.of(entity));

        final var response = service.getProduct(productId);

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.productName()).isEqualTo("Laptop");
    }

    @Test
    void rejectsDetailWhenProductMissingOrInactive() {
        final var productId = UUID.randomUUID();
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(productId))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updatesProductNameAndDescription() {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var entity = product(productId, category(categoryId), brand(UUID.randomUUID()),
            "Laptop", "old");
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.of(entity));
        when(repository.existsByNameInCategory(categoryId, "Gaming Laptop", productId))
            .thenReturn(false);
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        final var response = service.updateProduct(
            productId, new UpdateProductRequest("  Gaming Laptop  ", "new"));

        assertThat(response.productName()).isEqualTo("Gaming Laptop");
        assertThat(response.productDescription()).isEqualTo("new");
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsUpdateWhenNameExistsInCategory() {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var entity = product(productId, category(categoryId), brand(UUID.randomUUID()),
            "Laptop", "d");
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.of(entity));
        when(repository.existsByNameInCategory(categoryId, "Mouse", productId)).thenReturn(true);

        assertThatThrownBy(() -> service.updateProduct(
            productId, new UpdateProductRequest("Mouse", "d")))
            .isInstanceOf(ProductAlreadyExistsException.class);

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void rejectsUpdateWhenProductDoesNotExist() {
        final var productId = UUID.randomUUID();
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProduct(
            productId, new UpdateProductRequest("Laptop", null)))
            .isInstanceOf(ProductNotFoundException.class);

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void deactivatesProductAndPublishesEvent() {
        final var productId = UUID.randomUUID();
        final var entity = product(productId, category(UUID.randomUUID()), brand(UUID.randomUUID()),
            "Laptop", null);
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        service.deactivateProduct(productId);

        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
        final var captor = ArgumentCaptor.forClass(ProductDeactivatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(productId);
        assertThat(captor.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void treatsAlreadyInactiveProductAsSuccessfulDeactivation() {
        final var productId = UUID.randomUUID();
        final var entity = product(productId, category(UUID.randomUUID()), brand(UUID.randomUUID()),
            "Laptop", null);
        entity.setIsActive(false);
        when(repository.findById(productId)).thenReturn(Optional.of(entity));

        service.deactivateProduct(productId);

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
        verify(eventPublisher, never()).publishEvent(any(ProductDeactivatedEvent.class));
    }

    @Test
    void rejectsDeactivationWhenProductDoesNotExist() {
        final var productId = UUID.randomUUID();
        when(repository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateProduct(productId))
            .isInstanceOf(ProductNotFoundException.class);

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    private CategoryEntity category(final UUID categoryId) {
        final var entity = new CategoryEntity();
        entity.setCategoryId(categoryId);
        entity.setCategoryName("Laptops");
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private BrandEntity brand(final UUID brandId) {
        final var entity = new BrandEntity();
        entity.setBrandId(brandId);
        entity.setBrandName("Dell");
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private ProductEntity product(
            final UUID productId,
            final CategoryEntity category,
            final BrandEntity brand,
            final String name,
            final String description) {
        final var entity = new ProductEntity();
        entity.setProductId(productId);
        entity.setCategory(category);
        entity.setBrand(brand);
        entity.setProductName(name);
        entity.setProductDescription(description);
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }
}
