package app.ecommerce.sku.impl.service;

import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.impl.entity.ProductOptionEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import app.ecommerce.product.impl.repository.ProductOptionRepository;
import app.ecommerce.product.impl.repository.ProductOptionValueRepository;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.api.exceptions.InvalidVariantCombinationException;
import app.ecommerce.sku.api.exceptions.SkuAlreadyExistsException;
import app.ecommerce.sku.api.exceptions.SkuNotFoundException;
import app.ecommerce.sku.api.service.SkuService;
import app.ecommerce.sku.impl.mapper.SkuMapper;
import app.ecommerce.sku.impl.repository.SkuRepository;
import app.ecommerce.sku.impl.repository.SkuSpecifications;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

    private static final String SKU_CODE_CONSTRAINT = "uq_sku_code";

    private final SkuRepository repository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository optionRepository;
    private final ProductOptionValueRepository optionValueRepository;
    private final SkuMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;

    @Override
    @Transactional
    public SkuResponse createSku(final CreateSkuRequest request) {
        final var productId = request.productId();
        final var skuCode = request.skuCode().strip();
        log.debug("Creating SKU: productId={}", productId);

        final var product = productRepository.findByProductIdAndIsActiveTrue(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        if (repository.existsBySkuCode(skuCode)) {
            throw new SkuAlreadyExistsException(skuCode);
        }

        final var optionValues = validateCombination(productId, request.optionValueIds());

        final var now = clock.instant();
        final var sku = mapper.toNewEntity(product, request, skuCode, now);
        sku.setOptionValues(new HashSet<>(optionValues));

        try {
            final var saved = repository.saveAndFlush(sku);
            log.info("SKU created: skuId={}", saved.getSkuId());
            return mapper.toResponse(saved);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, SKU_CODE_CONSTRAINT)) {
                log.warn("Concurrent SKU creation conflict: constraint={}", SKU_CODE_CONSTRAINT);
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

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SkuResponse> getSkus(final UUID productId, final int page, final int size) {
        final var pageIndex = page - 1;
        final var sort = Sort
            .by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "skuId"));
        final var pageable = PageRequest.of(pageIndex, size, sort);
        log.debug("Getting active SKUs: productId={}, page={}, size={}", productId, page, size);

        final var entityPage = repository
            .findAll(SkuSpecifications.activeForProduct(productId), pageable)
            .map(mapper::toResponse);
        return PageResponse.from(entityPage);
    }

    @Override
    @Transactional
    public SkuResponse updateSku(final UUID skuId, final UpdateSkuRequest request) {
        log.debug("Updating SKU: skuId={}", skuId);

        final var entity = repository.findBySkuIdAndIsActiveTrue(skuId)
            .orElseThrow(() -> new SkuNotFoundException(skuId));

        mapper.update(entity, request, clock.instant());
        final var updated = repository.saveAndFlush(entity);
        log.info("SKU updated: skuId={}", updated.getSkuId());
        return mapper.toResponse(updated);
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

    /**
     * The chosen option values must form a valid variant: exactly one value for each option
     * of the product, no unknown values, no values from another product.
     *
     * @return the loaded option-value entities, to attach to the SKU
     */
    private List<ProductOptionValueEntity> validateCombination(
            final UUID productId, final List<UUID> requestedIds) {
        final var ids = requestedIds == null ? List.<UUID>of() : requestedIds;

        final Set<UUID> productOptionIds =
            optionRepository.findByProductOrderByPosition(productId).stream()
                .map(ProductOptionEntity::getOptionId)
                .collect(Collectors.toSet());

        if (new HashSet<>(ids).size() != ids.size()) {
            throw new InvalidVariantCombinationException("Duplicate option value in the combination");
        }

        final var values = optionValueRepository.findAllById(ids);
        if (values.size() != ids.size()) {
            throw new InvalidVariantCombinationException("One or more option values do not exist");
        }

        final var selectedOptionIds = values.stream()
            .map(value -> value.getOption().getOptionId())
            .toList();

        if (!productOptionIds.containsAll(selectedOptionIds)) {
            throw new InvalidVariantCombinationException(
                "An option value does not belong to this product");
        }
        if (new HashSet<>(selectedOptionIds).size() != selectedOptionIds.size()) {
            throw new InvalidVariantCombinationException(
                "Two values selected for the same option");
        }
        if (!new HashSet<>(selectedOptionIds).equals(productOptionIds)) {
            throw new InvalidVariantCombinationException(
                "Must select exactly one value for each option of the product");
        }
        return values;
    }
}
