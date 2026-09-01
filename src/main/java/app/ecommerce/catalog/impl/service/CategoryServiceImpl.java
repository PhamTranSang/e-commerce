package app.ecommerce.catalog.impl.service;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryResponse;
import app.ecommerce.catalog.api.dto.response.CategoryTreeResponse;
import app.ecommerce.catalog.api.event.CategoryDeactivatedEvent;
import app.ecommerce.catalog.api.exceptions.CategoryAlreadyExistsException;
import app.ecommerce.catalog.api.exceptions.CategoryNotFoundException;
import app.ecommerce.catalog.api.service.CategoryService;
import app.ecommerce.catalog.impl.entity.CategoryEntity;
import app.ecommerce.catalog.impl.mapper.CategoryMapper;
import app.ecommerce.catalog.impl.repository.CategoryRepository;
import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String ROOT_NAME_CONSTRAINT = "uq_category_root_name";
    private static final String CHILD_NAME_CONSTRAINT = "uq_category_child_name";

    private final CategoryRepository repository;
    private final CategoryMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CategoryResponse createCategory(final CreateCategoryRequest request) {
        final var categoryName = request.categoryName().strip();
        final var parentId = request.parentId();
        log.debug("Creating category: parentId={}", parentId);

        final CategoryEntity parent = resolveActiveParent(parentId);

        if (repository.existsSiblingWithName(categoryName, parentId, null)) {
            throw new CategoryAlreadyExistsException(categoryName);
        }

        final var normalizedRequest = new CreateCategoryRequest(categoryName, parentId);

        try {
            final var now = clock.instant();
            final var entity = repository.saveAndFlush(
                mapper.toNewEntity(normalizedRequest, parent, now));
            log.info("Category created: categoryId={}", entity.getCategoryId());
            return mapper.toResponse(entity);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, ROOT_NAME_CONSTRAINT)
                || constraintInspector.isViolationOf(e, CHILD_NAME_CONSTRAINT)) {
                log.warn("Concurrent category creation conflict: parentId={}", parentId);
                throw new CategoryAlreadyExistsException(categoryName, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(final UUID categoryId) {
        log.debug("Getting category: categoryId={}", categoryId);

        final var entity = repository.findByCategoryIdAndIsActiveTrue(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        log.debug("Getting category tree");

        final var active = repository.findByIsActiveTrue();
        final Map<UUID, List<CategoryEntity>> childrenByParent = active.stream()
            .filter(category -> category.getParent() != null)
            .collect(Collectors.groupingBy(category -> category.getParent().getCategoryId()));

        return active.stream()
            .filter(category -> category.getParent() == null)
            .map(root -> toTree(root, childrenByParent))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(final int page, final int size) {
        final var pageIndex = page - 1;
        final var sort = Sort
            .by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "categoryId"));
        final var pageable = PageRequest.of(pageIndex, size, sort);
        log.debug("Getting active categories: page={}, size={}", page, size);

        final var entityPage = repository.findAllByIsActiveTrue(pageable)
            .map(mapper::toResponse);
        return PageResponse.from(entityPage);
    }

    @Override
    @Transactional
    public CategoryResponse renameCategory(
            final UUID categoryId, final RenameCategoryRequest request) {
        log.debug("Renaming category: categoryId={}", categoryId);

        final var entity = repository.findByCategoryIdAndIsActiveTrue(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        final var categoryName = request.categoryName().strip();
        final UUID parentId =
            entity.getParent() == null ? null : entity.getParent().getCategoryId();

        if (repository.existsSiblingWithName(categoryName, parentId, categoryId)) {
            throw new CategoryAlreadyExistsException(categoryName);
        }

        mapper.rename(entity, new RenameCategoryRequest(categoryName), clock.instant());

        try {
            final var updated = repository.saveAndFlush(entity);
            log.info("Category renamed: categoryId={}", updated.getCategoryId());
            return mapper.toResponse(updated);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, ROOT_NAME_CONSTRAINT)
                || constraintInspector.isViolationOf(e, CHILD_NAME_CONSTRAINT)) {
                log.warn("Concurrent category rename conflict: categoryId={}", categoryId);
                throw new CategoryAlreadyExistsException(categoryName, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void deactivateCategory(final UUID categoryId) {
        log.debug("Deactivating category: categoryId={}", categoryId);

        final var entity = repository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (!entity.getIsActive()) {
            log.debug("Category already inactive: categoryId={}", categoryId);
            return;
        }

        final var now = clock.instant();
        mapper.deactivate(entity, now);
        repository.saveAndFlush(entity);

        eventPublisher.publishEvent(new CategoryDeactivatedEvent(categoryId, now));

        log.info("Category deactivated: categoryId={}", categoryId);
    }

    private CategoryEntity resolveActiveParent(final UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return repository.findByCategoryIdAndIsActiveTrue(parentId)
            .orElseThrow(() -> new CategoryNotFoundException(parentId));
    }

    private CategoryTreeResponse toTree(
            final CategoryEntity node, final Map<UUID, List<CategoryEntity>> childrenByParent) {
        final var children = childrenByParent.getOrDefault(node.getCategoryId(), List.of())
            .stream()
            .map(child -> toTree(child, childrenByParent))
            .toList();
        return new CategoryTreeResponse(node.getCategoryId(), node.getCategoryName(), children);
    }
}
