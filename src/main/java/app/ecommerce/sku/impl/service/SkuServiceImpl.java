package app.ecommerce.sku.impl.service;

import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.api.exceptions.SkuAlreadyExistsException;
import app.ecommerce.sku.api.exceptions.SkuNotFoundException;
import app.ecommerce.sku.api.service.SkuService;
import app.ecommerce.sku.impl.mapper.SkuMapper;
import app.ecommerce.sku.impl.repository.SkuRepository;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private static final String SKU_CODE_UNIQUE_CONSTRAINT = "uq_sku_code";

    private final SkuRepository repository;
    private final ProductRepository productRepository;
    private final SkuMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;

    @Override
    @Transactional
    public SkuResponse createSku(final CreateSkuRequest request) {
        log.debug("Creating SKU: productId={}", request.productId());

        final var skuCode = request.skuCode().strip();
        final var product = productRepository
            .findByProductIdAndIsActiveTrue(request.productId())
            .orElseThrow(() -> new ProductNotFoundException(request.productId()));

        if (repository.existsBySkuCode(skuCode)) {
            throw new SkuAlreadyExistsException(skuCode);
        }

        final var normalizedRequest = new CreateSkuRequest(
            request.productId(),
            skuCode,
            request.weightGrams(),
            request.amount(),
            request.currency()
        );

        try {
            final var now = clock.instant();
            final var entity = repository.saveAndFlush(
                mapper.toNewEntity(normalizedRequest, product, now));
            log.info("SKU created: skuId={}", entity.getSkuId());
            return mapper.toResponse(entity);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, SKU_CODE_UNIQUE_CONSTRAINT)) {
                log.warn(
                    "Concurrent SKU creation conflict: constraint={}",
                    SKU_CODE_UNIQUE_CONSTRAINT
                );
                throw new SkuAlreadyExistsException(skuCode, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SkuResponse getSku(final UUID skuId) {
        log.debug("Getting SKU: skuId={}", skuId);

        final var entity = repository.findBySkuIdAndIsActiveTrue(skuId)
            .orElseThrow(() -> new SkuNotFoundException(skuId));

        log.debug("SKU retrieved: skuId={}", entity.getSkuId());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SkuResponse> getSkus(
            final UUID productId, final int page, final int size) {
        final var pageIndex = page - 1;

        final var sort = Sort
            .by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "skuId"));
        final var pageable = PageRequest.of(pageIndex, size, sort);
        log.debug("Getting active SKUs: productId={}, page={}, size={}", productId, page, size);

        final var entityPage = repository
            .findAllByProduct_ProductIdAndIsActiveTrue(productId, pageable)
            .map(mapper::toResponse);

        log.debug(
            "Active SKUs retrieved: productId={}, page={}, size={}, numberOfElements={}, "
                + "totalElements={}, totalPages={}",
            productId,
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
    public SkuResponse updateSku(final UUID skuId, final UpdateSkuRequest request) {
        log.debug("Updating SKU: skuId={}", skuId);

        final var entity = repository.findBySkuIdAndIsActiveTrue(skuId)
            .orElseThrow(() -> new SkuNotFoundException(skuId));

        mapper.update(entity, request, clock.instant());

        final var updatedEntity = repository.saveAndFlush(entity);
        log.info("SKU updated: skuId={}", updatedEntity.getSkuId());
        return mapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void deactivateSku(final UUID skuId) {
        log.debug("Deactivating SKU: skuId={}", skuId);

        final var entity = repository.findById(skuId)
            .orElseThrow(() -> new SkuNotFoundException(skuId));

        if (!entity.getIsActive()) {
            log.debug("SKU already inactive: skuId={}", skuId);
            return;
        }

        mapper.deactivate(entity, clock.instant());
        repository.saveAndFlush(entity);

        log.info("SKU deactivated: skuId={}", skuId);
    }
}
