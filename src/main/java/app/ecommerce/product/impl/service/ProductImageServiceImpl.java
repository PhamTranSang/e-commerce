package app.ecommerce.product.impl.service;

import app.ecommerce.product.api.dto.request.CreateProductImageRequest;
import app.ecommerce.product.api.dto.response.ProductImageResponse;
import app.ecommerce.product.api.exceptions.OptionValueNotInProductException;
import app.ecommerce.product.api.exceptions.ProductImageAlreadyHasPrimaryException;
import app.ecommerce.product.api.exceptions.ProductNotFoundException;
import app.ecommerce.product.api.service.ProductImageService;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import app.ecommerce.product.impl.mapper.ProductImageMapper;
import app.ecommerce.product.impl.repository.ProductImageRepository;
import app.ecommerce.product.impl.repository.ProductOptionValueRepository;
import app.ecommerce.product.impl.repository.ProductRepository;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private static final String PRIMARY_IMAGE_CONSTRAINT = "uq_product_image_primary";

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ProductOptionValueRepository optionValueRepository;
    private final ProductImageMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;

    @Override
    @Transactional
    public ProductImageResponse addImage(
            final UUID productId, final CreateProductImageRequest request) {
        log.debug("Adding image to product: productId={}", productId);

        final var product = productRepository.findByProductIdAndIsActiveTrue(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        final var url = request.url().strip();
        final var altText = request.altText() == null ? null : request.altText().strip();
        final var optionValue = resolveOptionValue(request.optionValueId(), productId);
        final var primary = Boolean.TRUE.equals(request.isPrimary());

        if (primary && imageRepository.existsPrimaryForProduct(productId)) {
            throw new ProductImageAlreadyHasPrimaryException(productId);
        }

        final var now = clock.instant();
        final var position = (int) imageRepository.countByProduct(productId);
        final var image =
            mapper.toNewEntity(product, optionValue, url, altText, position, primary, now);

        try {
            final var saved = imageRepository.saveAndFlush(image);
            log.info("Image added: productId={}, imageId={}", productId, saved.getImageId());
            return mapper.toResponse(saved);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, PRIMARY_IMAGE_CONSTRAINT)) {
                log.warn("Concurrent primary image conflict: productId={}", productId);
                throw new ProductImageAlreadyHasPrimaryException(productId, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getProductImages(final UUID productId) {
        log.debug("Getting images of product: productId={}", productId);

        if (productRepository.findByProductIdAndIsActiveTrue(productId).isEmpty()) {
            throw new ProductNotFoundException(productId);
        }

        return imageRepository.findByProductOrderByPosition(productId).stream()
            .map(mapper::toResponse)
            .toList();
    }

    private ProductOptionValueEntity resolveOptionValue(
            final UUID optionValueId, final UUID productId) {
        if (optionValueId == null) {
            return null;
        }
        if (!optionValueRepository.existsForProduct(optionValueId, productId)) {
            throw new OptionValueNotInProductException(optionValueId, productId);
        }
        return optionValueRepository.getReferenceById(optionValueId);
    }
}
