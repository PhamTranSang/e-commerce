package app.ecommerce.catalog.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.exceptions.CategoryAlreadyExistsException;
import app.ecommerce.catalog.api.exceptions.CategoryNotFoundException;
import app.ecommerce.catalog.impl.entity.CategoryEntity;
import app.ecommerce.catalog.impl.mapper.CategoryMapper;
import app.ecommerce.catalog.impl.repository.CategoryRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CategoryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryServiceImpl service =
        new CategoryServiceImpl(
            repository,
            new CategoryMapper(),
            CLOCK,
            new DatabaseConstraintInspector()
        );

    @Test
    void createsCategoryWithNormalizedNameAndServerManagedFields() {
        final var categoryId = UUID.randomUUID();
        when(repository.existsByCategoryNameIgnoreCase("Electronics")).thenReturn(false);
        when(repository.saveAndFlush(any(CategoryEntity.class))).thenAnswer(invocation -> {
            final CategoryEntity entity = invocation.getArgument(0);
            entity.setCategoryId(categoryId);
            return entity;
        });

        final var response = service.createCategory(new CreateCategoryRequest("  Electronics  "));

        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.categoryName()).isEqualTo("Electronics");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
        verify(repository).existsByCategoryNameIgnoreCase("Electronics");
    }

    @Test
    void rejectsCategoryWhenNormalizedNameAlreadyExists() {
        when(repository.existsByCategoryNameIgnoreCase("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CreateCategoryRequest(" Electronics ")))
            .isInstanceOf(CategoryAlreadyExistsException.class)
            .hasMessage("Category 'Electronics' already exists");

        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    @Test
    void translatesConcurrentUniqueConstraintViolationToBusinessConflict() {
        when(repository.existsByCategoryNameIgnoreCase("Electronics")).thenReturn(false);
        final var constraintViolation = new ConstraintViolationException(
            "duplicate category name",
            new SQLException(),
            "uq_category_name_normalized"
        );
        final var databaseException =
            new DataIntegrityViolationException("duplicate category name", constraintViolation);
        when(repository.saveAndFlush(any(CategoryEntity.class)))
            .thenThrow(databaseException);

        assertThatThrownBy(() -> service.createCategory(new CreateCategoryRequest("Electronics")))
            .isInstanceOf(CategoryAlreadyExistsException.class)
            .hasMessage("Category 'Electronics' already exists")
            .hasCause(databaseException);
    }

    @Test
    void getsActiveCategoryDetail() {
        final var categoryId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = category(categoryId, "Electronics", createdAt, NOW);
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(entity));

        final var response = service.getCategory(categoryId);

        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.categoryName()).isEqualTo("Electronics");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsCategoryDetailWhenCategoryIsMissingOrInactive() {
        final var categoryId = UUID.randomUUID();
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategory(categoryId))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessage("Category '%s' was not found".formatted(categoryId));
    }

    @Test
    void getsActiveCategoriesUsingOneBasedPageAndStableSort() {
        final var firstCategory = category(
            UUID.randomUUID(),
            "Electronics",
            Instant.parse("2026-08-16T10:00:00Z"),
            NOW
        );
        final var secondCategory = category(
            UUID.randomUUID(),
            "Appliances",
            Instant.parse("2026-08-15T10:00:00Z"),
            NOW
        );
        final var sort = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "categoryId"));
        final var pageable = PageRequest.of(1, 2, sort);
        when(repository.findAllByIsActiveTrue(pageable)).thenReturn(
            new PageImpl<>(List.of(firstCategory, secondCategory), pageable, 4)
        );

        final var response = service.getCategories(2, 2);

        assertThat(response.content())
            .extracting(categoryResponse -> categoryResponse.categoryName())
            .containsExactly("Electronics", "Appliances");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.numberOfElements()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(4);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isTrue();
        verify(repository).findAllByIsActiveTrue(pageable);
    }

    @Test
    void renamesCategoryAndUpdatesTimestampWhilePreservingServerManagedFields() {
        final var categoryId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = category(categoryId, "Electronics", createdAt, createdAt);
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(entity));
        when(repository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(
            "Consumer Electronics",
            categoryId
        )).thenReturn(false);
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        final var response = service.renameCategory(
            categoryId,
            new RenameCategoryRequest("  Consumer Electronics  ")
        );

        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.categoryName()).isEqualTo("Consumer Electronics");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void rejectsRenameWhenNameBelongsToAnotherCategory() {
        final var categoryId = UUID.randomUUID();
        final var originalUpdatedAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = category(
            categoryId,
            "Electronics",
            originalUpdatedAt,
            originalUpdatedAt
        );
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.of(entity));
        when(repository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(
            "Appliances",
            categoryId
        )).thenReturn(true);

        assertThatThrownBy(() -> service.renameCategory(
            categoryId,
            new RenameCategoryRequest(" Appliances ")
        ))
            .isInstanceOf(CategoryAlreadyExistsException.class)
            .hasMessage("Category 'Appliances' already exists");

        assertThat(entity.getCategoryName()).isEqualTo("Electronics");
        assertThat(entity.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    @Test
    void rejectsRenameWhenCategoryDoesNotExist() {
        final var categoryId = UUID.randomUUID();
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renameCategory(
            categoryId,
            new RenameCategoryRequest("Electronics")
        ))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessage("Category '%s' was not found".formatted(categoryId));

        verify(repository, never())
            .existsByCategoryNameIgnoreCaseAndCategoryIdNot(any(), any());
        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    @Test
    void deactivatesCategoryAndUpdatesTimestamp() {
        final var categoryId = UUID.randomUUID();
        final var createdAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = category(categoryId, "Electronics", createdAt, createdAt);
        when(repository.findById(categoryId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        service.deactivateCategory(categoryId);

        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void treatsAlreadyInactiveCategoryAsSuccessfulDeactivation() {
        final var categoryId = UUID.randomUUID();
        final var updatedAt = Instant.parse("2026-08-01T10:00:00Z");
        final var entity = category(categoryId, "Electronics", updatedAt, updatedAt);
        entity.setIsActive(false);
        when(repository.findById(categoryId)).thenReturn(Optional.of(entity));

        service.deactivateCategory(categoryId);

        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    @Test
    void rejectsDeactivationWhenCategoryDoesNotExist() {
        final var categoryId = UUID.randomUUID();
        when(repository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateCategory(categoryId))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessage("Category '%s' was not found".formatted(categoryId));

        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    private CategoryEntity category(
            final UUID categoryId,
            final String categoryName,
            final Instant createdAt,
            final Instant updatedAt) {
        final var entity = new CategoryEntity();
        entity.setCategoryId(categoryId);
        entity.setCategoryName(categoryName);
        entity.setIsActive(true);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }
}
