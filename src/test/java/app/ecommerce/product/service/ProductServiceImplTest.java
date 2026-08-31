package app.ecommerce.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.product.api.dto.request.UpdateProductRequest;
import app.ecommerce.catalog.api.exceptions.CategoryNotFoundException;
import app.ecommerce.product.api.dto.response.ProductResponse;
import app.ecommerce.product.api.event.ProductDeactivatedEvent;
import app.ecommerce.product.api.exceptions.ProductAlreadyExistsException;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.catalog.impl.entity.CategoryEntity;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.mapper.ProductMapper;
import app.ecommerce.catalog.impl.repository.CategoryRepository;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.product.impl.service.ProductServiceImpl;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class ProductServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ProductRepository repository = mock(ProductRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ProductServiceImpl service =
        new ProductServiceImpl(
            repository,
            categoryRepository,
            new ProductMapper(),
            CLOCK,
            new DatabaseConstraintInspector(),
            eventPublisher
        );

    @Test
    void createsProductWithNormalizedNameAndServerManagedFields() {
        final var categoryId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        final var category = category(categoryId);
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(category));
        when(repository.existsByCategory_CategoryIdAndProductNameIgnoreCase(categoryId, "Laptop"))
            .thenReturn(false);
        when(repository.saveAndFlush(any(ProductEntity.class))).thenAnswer(invocation -> {
            final ProductEntity entity = invocation.getArgument(0);
            entity.setProductId(productId);
            return entity;
        });

        final var response = service.createProduct(
            new CreateProductRequest(categoryId, "  Laptop  ", "  A portable computer  "));

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.productName()).isEqualTo("Laptop");
        assertThat(response.productDescription()).isEqualTo("  A portable computer  ");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsProductWhenCategoryIsMissingOrInactive() {
        final var categoryId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProduct(
            new CreateProductRequest(categoryId, "Laptop", null)))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessage("Category '%s' was not found".formatted(categoryId));

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void rejectsProductWhenNameAlreadyExistsInCategory() {
        final var categoryId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(category(categoryId)));
        when(repository.existsByCategory_CategoryIdAndProductNameIgnoreCase(categoryId, "Laptop"))
            .thenReturn(true);

        assertThatThrownBy(() -> service.createProduct(
            new CreateProductRequest(categoryId, " Laptop ", null)))
            .isInstanceOf(ProductAlreadyExistsException.class)
            .hasMessage("Product 'Laptop' already exists in this category");

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void translatesConcurrentUniqueConstraintViolationToBusinessConflict() {
        final var categoryId = UUID.randomUUID();
        when(categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(category(categoryId)));
        when(repository.existsByCategory_CategoryIdAndProductNameIgnoreCase(categoryId, "Laptop"))
            .thenReturn(false);
        final var constraintViolation = new ConstraintViolationException(
            "duplicate product name",
            new SQLException(),
            "uq_product_name_normalized"
        );
        final var databaseException =
            new DataIntegrityViolationException("duplicate product name", constraintViolation);
        when(repository.saveAndFlush(any(ProductEntity.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.createProduct(
            new CreateProductRequest(categoryId, "Laptop", null)))
            .isInstanceOf(ProductAlreadyExistsException.class)
            .hasMessage("Product 'Laptop' already exists in this category")
            .hasCause(databaseException);
    }

    @Test
    void getsActiveProductDetail() {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = product(productId, categoryId, "Laptop", "desc", createdAt, NOW);
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.of(entity));

        final var response = service.getProduct(productId);

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.productName()).isEqualTo("Laptop");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsProductDetailWhenProductIsMissingOrInactive() {
        final var productId = UUID.randomUUID();
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct(productId))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("Product '%s' was not found".formatted(productId));
    }

    @Test
    void getsActiveProductsUsingOneBasedPageAndStableSort() {
        final var categoryId = UUID.randomUUID();
        final var first = product(
            UUID.randomUUID(), categoryId, "Laptop", null,
            Instant.parse("2026-08-16T10:00:00Z"), NOW);
        final var second = product(
            UUID.randomUUID(), categoryId, "Mouse", null,
            Instant.parse("2026-08-15T10:00:00Z"), NOW);
        final var sort = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "productId"));
        final var pageable = PageRequest.of(1, 2, sort);
        when(repository.findAllByCategory_CategoryIdAndIsActiveTrue(categoryId, pageable))
            .thenReturn(new PageImpl<>(List.of(first, second), pageable, 4));

        final var response = service.getProducts(categoryId, 2, 2);

        assertThat(response.content())
            .extracting(ProductResponse::productName)
            .containsExactly("Laptop", "Mouse");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(4);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasPrevious()).isTrue();
        verify(repository).findAllByCategory_CategoryIdAndIsActiveTrue(categoryId, pageable);
    }

    @Test
    void updatesProductAndTimestampWhilePreservingServerManagedFields() {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = product(productId, categoryId, "Laptop", "old", createdAt, createdAt);
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.of(entity));
        when(repository.existsByCategory_CategoryIdAndProductNameIgnoreCaseAndProductIdNot(
            categoryId, "Gaming Laptop", productId)).thenReturn(false);
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        final var response = service.updateProduct(
            productId, new UpdateProductRequest("  Gaming Laptop  ", "new"));

        assertThat(response.productName()).isEqualTo("Gaming Laptop");
        assertThat(response.productDescription()).isEqualTo("new");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void rejectsUpdateWhenNameBelongsToAnotherProductInCategory() {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var originalUpdatedAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity =
            product(productId, categoryId, "Laptop", "d", originalUpdatedAt, originalUpdatedAt);
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.of(entity));
        when(repository.existsByCategory_CategoryIdAndProductNameIgnoreCaseAndProductIdNot(
            categoryId, "Mouse", productId)).thenReturn(true);

        assertThatThrownBy(() -> service.updateProduct(
            productId, new UpdateProductRequest(" Mouse ", "d")))
            .isInstanceOf(ProductAlreadyExistsException.class)
            .hasMessage("Product 'Mouse' already exists in this category");

        assertThat(entity.getProductName()).isEqualTo("Laptop");
        assertThat(entity.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void rejectsUpdateWhenProductDoesNotExist() {
        final var productId = UUID.randomUUID();
        when(repository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProduct(
            productId, new UpdateProductRequest("Laptop", null)))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("Product '%s' was not found".formatted(productId));

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    @Test
    void deactivatesProductAndUpdatesTimestamp() {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = product(productId, categoryId, "Laptop", null, createdAt, createdAt);
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        service.deactivateProduct(productId);

        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(entity);
        verify(eventPublisher).publishEvent(new ProductDeactivatedEvent(productId, NOW));
    }

    @Test
    void treatsAlreadyInactiveProductAsSuccessfulDeactivation() {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var updatedAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = product(productId, categoryId, "Laptop", null, updatedAt, updatedAt);
        entity.setIsActive(false);
        when(repository.findById(productId)).thenReturn(Optional.of(entity));

        service.deactivateProduct(productId);

        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
        verify(eventPublisher, never()).publishEvent(any(ProductDeactivatedEvent.class));
    }

    @Test
    void rejectsDeactivationWhenProductDoesNotExist() {
        final var productId = UUID.randomUUID();
        when(repository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateProduct(productId))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("Product '%s' was not found".formatted(productId));

        verify(repository, never()).saveAndFlush(any(ProductEntity.class));
    }

    private CategoryEntity category(final UUID categoryId) {
        final var entity = new CategoryEntity();
        entity.setCategoryId(categoryId);
        entity.setCategoryName("Electronics");
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private ProductEntity product(
            final UUID productId,
            final UUID categoryId,
            final String productName,
            final String productDescription,
            final Instant createdAt,
            final Instant updatedAt) {
        final var entity = new ProductEntity();
        entity.setProductId(productId);
        entity.setCategory(category(categoryId));
        entity.setProductName(productName);
        entity.setProductDescription(productDescription);
        entity.setIsActive(true);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }
}