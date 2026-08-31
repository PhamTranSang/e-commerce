package app.ecommerce.product.impl.service;

import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.product.api.dto.request.UpdateProductRequest;
import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.product.api.dto.response.ProductResponse;
import app.ecommerce.catalog.api.exceptions.CategoryNotFoundException;
import app.ecommerce.product.api.event.ProductDeactivatedEvent;
import app.ecommerce.product.api.exceptions.ProductAlreadyExistsException;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.api.service.ProductService;
import app.ecommerce.product.impl.mapper.ProductMapper;
import app.ecommerce.catalog.impl.repository.CategoryRepository;
import app.ecommerce.product.impl.repository.ProductRepository;
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
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_NAME_UNIQUE_CONSTRAINT = "uq_product_name_normalized";

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProductResponse createProduct(final CreateProductRequest request) {
        log.debug("Creating product: categoryId={}", request.categoryId());

        final var productName = request.productName().strip();
        final var category = categoryRepository
            .findByCategoryIdAndIsActiveTrue(request.categoryId())
            .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        if (repository.existsByCategory_CategoryIdAndProductNameIgnoreCase(
                request.categoryId(), productName)) {
            throw new ProductAlreadyExistsException(productName);
        }

        final var normalizedRequest = new CreateProductRequest(
            request.categoryId(),
            productName,
            request.productDescription()
        );

        try {
            final var now = clock.instant();
            final var entity = repository.saveAndFlush(
                mapper.toNewEntity(normalizedRequest, category, now));
            log.info("Product created: productId={}", entity.getProductId());
            return mapper.toResponse(entity);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, PRODUCT_NAME_UNIQUE_CONSTRAINT)) {
                log.warn(
                    "Concurrent product creation conflict: constraint={}",
                    PRODUCT_NAME_UNIQUE_CONSTRAINT
                );
                throw new ProductAlreadyExistsException(productName, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(final UUID productId) {
        log.debug("Getting product: productId={}", productId);

        final var entity = repository.findByProductIdAndIsActiveTrue(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        log.debug("Product retrieved: productId={}", entity.getProductId());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            final UUID categoryId, final int page, final int size) {
        final var pageIndex = page - 1;

        final var sort = Sort
            .by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "productId"));
        final var pageable = PageRequest.of(pageIndex, size, sort);
        log.debug("Getting active products: categoryId={}, page={}, size={}", categoryId, page, size);

        final var entityPage = repository
            .findAllByCategory_CategoryIdAndIsActiveTrue(categoryId, pageable)
            .map(mapper::toResponse);

        log.debug(
            "Active products retrieved: categoryId={}, page={}, size={}, numberOfElements={}, "
                + "totalElements={}, totalPages={}",
            categoryId,
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
    public ProductResponse updateProduct(
            final UUID productId, final UpdateProductRequest request) {
        log.debug("Updating product: productId={}", productId);

        final var entity = repository.findByProductIdAndIsActiveTrue(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        final var productName = request.productName().strip();
        final var categoryId = entity.getCategory().getCategoryId();

        if (repository.existsByCategory_CategoryIdAndProductNameIgnoreCaseAndProductIdNot(
                categoryId, productName, productId)) {
            throw new ProductAlreadyExistsException(productName);
        }

        mapper.update(
            entity,
            new UpdateProductRequest(productName, request.productDescription()),
            clock.instant()
        );

        try {
            final var updatedEntity = repository.saveAndFlush(entity);
            log.info("Product updated: productId={}", updatedEntity.getProductId());
            return mapper.toResponse(updatedEntity);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, PRODUCT_NAME_UNIQUE_CONSTRAINT)) {
                log.warn(
                    "Concurrent product update conflict: productId={}, constraint={}",
                    productId,
                    PRODUCT_NAME_UNIQUE_CONSTRAINT
                );
                throw new ProductAlreadyExistsException(productName, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void deactivateProduct(final UUID productId) {
        log.debug("Deactivating product: productId={}", productId);

        final var entity = repository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!entity.getIsActive()) {
            log.debug("Product already inactive: productId={}", productId);
            return;
        }

        final var now = clock.instant();
        mapper.deactivate(entity, now);
        repository.saveAndFlush(entity);

        eventPublisher.publishEvent(new ProductDeactivatedEvent(productId, now));

        log.info("Product deactivated: productId={}", productId);
    }
}
