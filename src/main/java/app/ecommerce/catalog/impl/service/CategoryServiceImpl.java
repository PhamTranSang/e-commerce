package app.ecommerce.catalog.impl.service;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryResponse;
import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.catalog.api.exceptions.CategoryAlreadyExistsException;
import app.ecommerce.catalog.api.exceptions.CategoryNotFoundException;
import app.ecommerce.catalog.api.event.CategoryDeactivatedEvent;
import app.ecommerce.catalog.api.service.CategoryService;
import app.ecommerce.catalog.impl.mapper.CategoryMapper;
import app.ecommerce.catalog.impl.repository.CategoryRepository;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String CATEGORY_NAME_UNIQUE_CONSTRAINT = "uq_category_name_normalized";

    private final CategoryRepository repository;
    private final CategoryMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CategoryResponse createCategory(final CreateCategoryRequest request) {
        log.debug("Creating category");

        final var categoryName = request.categoryName().strip();
        final var normalizedRequest = new CreateCategoryRequest(categoryName);

        if (repository.existsByCategoryNameIgnoreCase(categoryName)) {
            throw new CategoryAlreadyExistsException(categoryName);
        }

        try {
            final var now = clock.instant();
            final var entity = repository.saveAndFlush(mapper.toNewEntity(normalizedRequest, now));
            log.info("Category created: categoryId={}", entity.getCategoryId());
            return mapper.toResponse(entity);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, CATEGORY_NAME_UNIQUE_CONSTRAINT)) {
                log.warn(
                    "Concurrent category creation conflict: constraint={}",
                    CATEGORY_NAME_UNIQUE_CONSTRAINT
                );
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

        log.debug("Category retrieved: categoryId={}", entity.getCategoryId());
        return mapper.toResponse(entity);
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

        log.debug(
            "Active categories retrieved: page={}, size={}, numberOfElements={}, "
                + "totalElements={}, totalPages={}",
            page,
            size,
            entityPage.getNumberOfElements(),
            entityPage.getTotalElements(),
            entityPage.getTotalPages()
        );
        return PageResponse.from(entityPage);
    }

    @Override
    @Transactional
    public CategoryResponse renameCategory(
            final UUID categoryId,
            final RenameCategoryRequest request) {
        log.debug("Renaming category: categoryId={}", categoryId);

        final var entity = repository.findByCategoryIdAndIsActiveTrue(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        final var categoryName = request.categoryName().strip();

        if (repository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(categoryName, categoryId)) {
            throw new CategoryAlreadyExistsException(categoryName);
        }

        mapper.rename(
            entity,
            new RenameCategoryRequest(categoryName),
            clock.instant()
        );

        try {
            final var updatedEntity = repository.saveAndFlush(entity);
            log.info("Category renamed: categoryId={}", updatedEntity.getCategoryId());
            return mapper.toResponse(updatedEntity);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, CATEGORY_NAME_UNIQUE_CONSTRAINT)) {
                log.warn(
                    "Concurrent category rename conflict: categoryId={}, constraint={}",
                    categoryId,
                    CATEGORY_NAME_UNIQUE_CONSTRAINT
                );
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
}
