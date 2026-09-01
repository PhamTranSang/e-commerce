package app.ecommerce.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryTreeResponse;
import app.ecommerce.catalog.api.event.CategoryDeactivatedEvent;
import app.ecommerce.catalog.api.exceptions.CategoryAlreadyExistsException;
import app.ecommerce.catalog.api.exceptions.CategoryNotFoundException;
import app.ecommerce.catalog.impl.entity.CategoryEntity;
import app.ecommerce.catalog.impl.mapper.CategoryMapper;
import app.ecommerce.catalog.impl.repository.CategoryRepository;
import app.ecommerce.catalog.impl.service.CategoryServiceImpl;
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
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

class CategoryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final CategoryServiceImpl service =
        new CategoryServiceImpl(
            repository,
            new CategoryMapper(),
            CLOCK,
            new DatabaseConstraintInspector(),
            eventPublisher
        );

    @Test
    void createsRootCategoryWithNormalizedNameAndServerManagedFields() {
        final var categoryId = UUID.randomUUID();
        when(repository.existsSiblingWithName("Electronics", null, null)).thenReturn(false);
        when(repository.saveAndFlush(any(CategoryEntity.class))).thenAnswer(invocation -> {
            final CategoryEntity entity = invocation.getArgument(0);
            entity.setCategoryId(categoryId);
            return entity;
        });

        final var response = service.createCategory(
            new CreateCategoryRequest("  Electronics  ", null));

        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.parentId()).isNull();
        assertThat(response.categoryName()).isEqualTo("Electronics");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void createsChildCategoryUnderActiveParent() {
        final var parentId = UUID.randomUUID();
        final var childId = UUID.randomUUID();
        final var parent = category(parentId, "Electronics", null);
        when(repository.findByCategoryIdAndIsActiveTrue(parentId)).thenReturn(Optional.of(parent));
        when(repository.existsSiblingWithName("Smartphone", parentId, null)).thenReturn(false);
        when(repository.saveAndFlush(any(CategoryEntity.class))).thenAnswer(invocation -> {
            final CategoryEntity entity = invocation.getArgument(0);
            entity.setCategoryId(childId);
            return entity;
        });

        final var response = service.createCategory(
            new CreateCategoryRequest("Smartphone", parentId));

        assertThat(response.categoryId()).isEqualTo(childId);
        assertThat(response.parentId()).isEqualTo(parentId);
        assertThat(response.categoryName()).isEqualTo("Smartphone");
    }

    @Test
    void rejectsChildWhenParentIsMissingOrInactive() {
        final var parentId = UUID.randomUUID();
        when(repository.findByCategoryIdAndIsActiveTrue(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCategory(
            new CreateCategoryRequest("Smartphone", parentId)))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessage("Category '%s' was not found".formatted(parentId));

        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    @Test
    void rejectsCategoryWhenNameTakenUnderSameRoot() {
        when(repository.existsSiblingWithName("Electronics", null, null)).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(
            new CreateCategoryRequest(" Electronics ", null)))
            .isInstanceOf(CategoryAlreadyExistsException.class);

        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    @Test
    void translatesConcurrentUniqueConstraintViolationToConflict() {
        when(repository.existsSiblingWithName("Electronics", null, null)).thenReturn(false);
        final var constraintViolation = new ConstraintViolationException(
            "duplicate category name", new SQLException(), "uq_category_root_name");
        final var databaseException =
            new DataIntegrityViolationException("duplicate category name", constraintViolation);
        when(repository.saveAndFlush(any(CategoryEntity.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.createCategory(
            new CreateCategoryRequest("Electronics", null)))
            .isInstanceOf(CategoryAlreadyExistsException.class)
            .hasCause(databaseException);
    }

    @Test
    void getsActiveCategoryDetail() {
        final var categoryId = UUID.randomUUID();
        final var entity = category(categoryId, "Electronics", null);
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(entity));

        final var response = service.getCategory(categoryId);

        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.categoryName()).isEqualTo("Electronics");
    }

    @Test
    void rejectsDetailWhenCategoryMissingOrInactive() {
        final var categoryId = UUID.randomUUID();
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategory(categoryId))
            .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void buildsNestedCategoryTree() {
        final var electronicsId = UUID.randomUUID();
        final var electronics = category(electronicsId, "Electronics", null);
        final var smartphone = category(UUID.randomUUID(), "Smartphone", electronics);
        final var laptop = category(UUID.randomUUID(), "Laptop", electronics);
        final var standalone = category(UUID.randomUUID(), "Accessories", null);
        when(repository.findByIsActiveTrue())
            .thenReturn(List.of(electronics, smartphone, laptop, standalone));

        final var tree = service.getCategoryTree();

        assertThat(tree).extracting(CategoryTreeResponse::categoryName)
            .containsExactlyInAnyOrder("Electronics", "Accessories");
        final var electronicsNode = tree.stream()
            .filter(node -> node.categoryName().equals("Electronics"))
            .findFirst().orElseThrow();
        assertThat(electronicsNode.children()).extracting(CategoryTreeResponse::categoryName)
            .containsExactlyInAnyOrder("Smartphone", "Laptop");
        final var accessoriesNode = tree.stream()
            .filter(node -> node.categoryName().equals("Accessories"))
            .findFirst().orElseThrow();
        assertThat(accessoriesNode.children()).isEmpty();
    }

    @Test
    void renamesCategoryCheckingSiblingUniqueness() {
        final var parentId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var parent = category(parentId, "Electronics", null);
        final var entity = category(categoryId, "Smartphone", parent);
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(entity));
        when(repository.existsSiblingWithName("Mobile Phone", parentId, categoryId))
            .thenReturn(false);
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        final var response = service.renameCategory(
            categoryId, new RenameCategoryRequest("  Mobile Phone  "));

        assertThat(response.categoryName()).isEqualTo("Mobile Phone");
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsRenameWhenNameBelongsToSibling() {
        final var parentId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var parent = category(parentId, "Electronics", null);
        final var entity = category(categoryId, "Smartphone", parent);
        when(repository.findByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(entity));
        when(repository.existsSiblingWithName("Laptop", parentId, categoryId)).thenReturn(true);

        assertThatThrownBy(() -> service.renameCategory(
            categoryId, new RenameCategoryRequest("Laptop")))
            .isInstanceOf(CategoryAlreadyExistsException.class);

        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    @Test
    void deactivatesCategoryAndPublishesEvent() {
        final var categoryId = UUID.randomUUID();
        final var entity = category(categoryId, "Electronics", null);
        when(repository.findById(categoryId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        service.deactivateCategory(categoryId);

        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(entity);
        final var captor = ArgumentCaptor.forClass(CategoryDeactivatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().categoryId()).isEqualTo(categoryId);
        assertThat(captor.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void treatsAlreadyInactiveCategoryAsSuccessfulDeactivation() {
        final var categoryId = UUID.randomUUID();
        final var entity = category(categoryId, "Electronics", null);
        entity.setIsActive(false);
        when(repository.findById(categoryId)).thenReturn(Optional.of(entity));

        service.deactivateCategory(categoryId);

        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
        verify(eventPublisher, never()).publishEvent(any(CategoryDeactivatedEvent.class));
    }

    @Test
    void rejectsDeactivationWhenCategoryDoesNotExist() {
        final var categoryId = UUID.randomUUID();
        when(repository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateCategory(categoryId))
            .isInstanceOf(CategoryNotFoundException.class);

        verify(repository, never()).saveAndFlush(any(CategoryEntity.class));
    }

    private CategoryEntity category(
            final UUID categoryId, final String name, final CategoryEntity parent) {
        final var entity = new CategoryEntity();
        entity.setCategoryId(categoryId);
        entity.setParent(parent);
        entity.setCategoryName(name);
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }
}
