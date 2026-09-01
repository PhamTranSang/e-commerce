package app.ecommerce.product.impl.service;

import app.ecommerce.product.api.dto.request.CreateProductOptionRequest;
import app.ecommerce.product.api.dto.response.ProductOptionResponse;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.api.exceptions.ProductOptionAlreadyExistsException;
import app.ecommerce.product.api.exceptions.ProductOptionValueAlreadyExistsException;
import app.ecommerce.product.api.service.ProductOptionService;
import app.ecommerce.product.impl.entity.ProductOptionEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import app.ecommerce.product.impl.mapper.ProductOptionMapper;
import app.ecommerce.product.impl.repository.ProductOptionRepository;
import app.ecommerce.product.impl.repository.ProductOptionValueRepository;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductOptionServiceImpl implements ProductOptionService {

    private static final String OPTION_NAME_CONSTRAINT = "uq_option_name_per_product";

    private final ProductRepository productRepository;
    private final ProductOptionRepository optionRepository;
    private final ProductOptionValueRepository valueRepository;
    private final ProductOptionMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;

    @Override
    @Transactional
    public ProductOptionResponse addOption(final UUID productId, final CreateProductOptionRequest request) {
        log.debug("Adding option to product: productId={}", productId);

        final var product = productRepository.findByProductIdAndIsActiveTrue(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        final var optionName = request.optionName().strip();
        if (optionRepository.existsByProductAndName(productId, optionName)) {
            throw new ProductOptionAlreadyExistsException(optionName);
        }

        final var values = request.values().stream().map(String::strip).toList();
        rejectDuplicateValues(values);

        final var now = clock.instant();
        final var position = (int) optionRepository.countByProduct(productId);
        final var option = mapper.toNewOption(product, optionName, position, now);

        try {
            optionRepository.saveAndFlush(option);
            final var valueEntities = IntStream.range(0, values.size())
                .mapToObj(index -> mapper.toNewValue(option, values.get(index), index, now))
                .toList();
            valueRepository.saveAll(valueEntities);
            valueRepository.flush();
            log.info("Option added: productId={}, optionId={}", productId, option.getOptionId());
            return mapper.toResponse(option, valueEntities);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, OPTION_NAME_CONSTRAINT)) {
                log.warn("Concurrent option creation conflict: productId={}", productId);
                throw new ProductOptionAlreadyExistsException(optionName, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductOptionResponse> getProductOptions(final UUID productId) {
        log.debug("Getting options of product: productId={}", productId);

        if (productRepository.findByProductIdAndIsActiveTrue(productId).isEmpty()) {
            throw new ProductNotFoundException(productId);
        }

        final var options = optionRepository.findByProductOrderByPosition(productId);
        if (options.isEmpty()) {            return List.of();
        }

        final var optionIds = options.stream()
            .map(ProductOptionEntity::getOptionId)
            .toList();
        final Map<UUID, List<ProductOptionValueEntity>> valuesByOption =
            valueRepository.findByOptionsOrderByPosition(optionIds).stream()
                .collect(Collectors.groupingBy(value -> value.getOption().getOptionId()));

        return options.stream()
            .map(option -> mapper.toResponse(
                option, valuesByOption.getOrDefault(option.getOptionId(), List.of())))
            .toList();
    }

    private void rejectDuplicateValues(final List<String> values) {
        final var seen = new HashSet<String>();
        for (final var value : values) {
            if (!seen.add(value)) {
                throw new ProductOptionValueAlreadyExistsException(value);
            }
        }
    }
}