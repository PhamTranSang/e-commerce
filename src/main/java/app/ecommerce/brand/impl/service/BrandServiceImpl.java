package app.ecommerce.brand.impl.service;

import app.ecommerce.brand.api.dto.request.CreateBrandRequest;
import app.ecommerce.brand.api.dto.request.RenameBrandRequest;
import app.ecommerce.brand.api.dto.response.BrandResponse;
import app.ecommerce.brand.api.exceptions.BrandAlreadyExistsException;
import app.ecommerce.brand.api.exceptions.BrandNotFoundException;
import app.ecommerce.brand.api.service.BrandService;
import app.ecommerce.brand.impl.mapper.BrandMapper;
import app.ecommerce.brand.impl.repository.BrandRepository;
import app.ecommerce.shared.api.dto.response.PageResponse;
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
public class BrandServiceImpl implements BrandService {

    private static final String BRAND_NAME_CONSTRAINT = "uq_brand_name";

    private final BrandRepository repository;
    private final BrandMapper mapper;
    private final Clock clock;
    private final DatabaseConstraintInspector constraintInspector;

    @Override
    @Transactional
    public BrandResponse createBrand(final CreateBrandRequest request) {
        final var brandName = request.brandName().strip();
        log.debug("Creating brand");

        if (repository.existsByNameIgnoringCase(brandName, null)) {
            throw new BrandAlreadyExistsException(brandName);
        }

        final var normalizedRequest = new CreateBrandRequest(brandName);

        try {
            final var now = clock.instant();
            final var entity = repository.saveAndFlush(mapper.toNewEntity(normalizedRequest, now));
            log.info("Brand created: brandId={}", entity.getBrandId());
            return mapper.toResponse(entity);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, BRAND_NAME_CONSTRAINT)) {
                log.warn("Concurrent brand creation conflict: constraint={}", BRAND_NAME_CONSTRAINT);
                throw new BrandAlreadyExistsException(brandName, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrand(final UUID brandId) {
        log.debug("Getting brand: brandId={}", brandId);

        final var entity = repository.findByBrandIdAndIsActiveTrue(brandId)
            .orElseThrow(() -> new BrandNotFoundException(brandId));

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> getBrands(final int page, final int size) {
        final var pageIndex = page - 1;
        final var sort = Sort
            .by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "brandId"));
        final var pageable = PageRequest.of(pageIndex, size, sort);
        log.debug("Getting active brands: page={}, size={}", page, size);

        final var entityPage = repository.findAllByIsActiveTrue(pageable)
            .map(mapper::toResponse);
        return PageResponse.from(entityPage);
    }

    @Override
    @Transactional
    public BrandResponse renameBrand(final UUID brandId, final RenameBrandRequest request) {
        log.debug("Renaming brand: brandId={}", brandId);

        final var entity = repository.findByBrandIdAndIsActiveTrue(brandId)
            .orElseThrow(() -> new BrandNotFoundException(brandId));
        final var brandName = request.brandName().strip();

        if (repository.existsByNameIgnoringCase(brandName, brandId)) {
            throw new BrandAlreadyExistsException(brandName);
        }

        mapper.rename(entity, new RenameBrandRequest(brandName), clock.instant());

        try {
            final var updated = repository.saveAndFlush(entity);
            log.info("Brand renamed: brandId={}", updated.getBrandId());
            return mapper.toResponse(updated);
        } catch (final DataIntegrityViolationException e) {
            if (constraintInspector.isViolationOf(e, BRAND_NAME_CONSTRAINT)) {
                log.warn("Concurrent brand rename conflict: brandId={}", brandId);
                throw new BrandAlreadyExistsException(brandName, e);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void deactivateBrand(final UUID brandId) {
        log.debug("Deactivating brand: brandId={}", brandId);

        final var entity = repository.findById(brandId)
            .orElseThrow(() -> new BrandNotFoundException(brandId));

        if (!entity.getIsActive()) {
            log.debug("Brand already inactive: brandId={}", brandId);
            return;
        }

        mapper.deactivate(entity, clock.instant());
        repository.saveAndFlush(entity);

        log.info("Brand deactivated: brandId={}", brandId);
    }
}
