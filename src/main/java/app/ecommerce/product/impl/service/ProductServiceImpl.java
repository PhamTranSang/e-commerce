package app.ecommerce.product.impl.service;

import app.ecommerce.brand.api.exceptions.BrandNotFoundException;
import app.ecommerce.brand.impl.repository.BrandRepository;
import app.ecommerce.catalog.api.exceptions.CategoryNotFoundException;
import app.ecommerce.catalog.impl.repository.CategoryRepository;
import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.product.api.dto.request.UpdateProductRequest;
import app.ecommerce.product.api.dto.response.ProductResponse;
import app.ecommerce.product.api.event.ProductDeactivatedEvent;
import app.ecommerce.product.api.exceptions.LeafCategoryRequiredException;
import app.ecommerce.product.api.exceptions.ProductAlreadyExistsException;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.api.service.ProductService;
import app.ecommerce.product.impl.mapper.ProductMapper;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.product.impl.repository.ProductSpecifications;
import app.ecommerce.shared.api.dto.response.PageResponse;
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

    private static final String PRODUCT_NAME_CONSTRAINT = "uq_product_name";

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProductResponse createProduct(final CreateProductRequest request) {
        final var productName = request.productName().strip();
        final var categoryId = request.categoryId();
        final var brandId = request.brandId();
        log.debug("Creating product: categoryId={}, brandId={}", categoryId, brandId);

        final var category = categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        if (categoryRepository.hasChildren(categoryId)) {
            throw new LeafCategoryRequiredException(categoryId);
        }
        final var brand = brandRepository.findByBrandIdAndIsActiveTrue(brandId)
            .orElseThrow(() -> new BrandNotFoundException(brandId));

        if (repository.existsByNameInCategory(categoryId, productName, null)) {
            throw new ProductAlreadyExistsException(productName);
        }

        final var normalizedRequest = new CreateProductRequest(
            categoryId, brandId, productName, request.productDescription());

        try {
            final var now = clock.instant();
            final var entity = repository.saveAndFlush(
                mapper.toNewEntity(normalizedRequest, category, brand, now));
            log.info("Product created: productId={}", entity.getProductId());
            return mapper.toResponse(entity);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, PRODUCT_NAME_CONSTRAINT)) {
                log.warn("Concurrent product creation conflict: categoryId={}", categoryId);
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
            .findAll(ProductSpecifications.activeInCategory(categoryId), pageable)
            .map(mapper::toResponse);
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

        if (repository.existsByNameInCategory(categoryId, productName, productId)) {
            throw new ProductAlreadyExistsException(productName);
        }

        mapper.update(
            entity,
            new UpdateProductRequest(productName, request.productDescription()),
            clock.instant()
        );

        try {
            final var updated = repository.saveAndFlush(entity);
            log.info("Product updated: productId={}", updated.getProductId());
            return mapper.toResponse(updated);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, PRODUCT_NAME_CONSTRAINT)) {
                log.warn("Concurrent product update conflict: productId={}", productId);
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
