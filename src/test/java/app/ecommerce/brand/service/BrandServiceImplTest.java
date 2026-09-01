package app.ecommerce.brand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.ecommerce.brand.api.dto.request.CreateBrandRequest;
import app.ecommerce.brand.api.dto.request.RenameBrandRequest;
import app.ecommerce.brand.api.exceptions.BrandAlreadyExistsException;
import app.ecommerce.brand.api.exceptions.BrandNotFoundException;
import app.ecommerce.brand.impl.entity.BrandEntity;
import app.ecommerce.brand.impl.mapper.BrandMapper;
import app.ecommerce.brand.impl.repository.BrandRepository;
import app.ecommerce.brand.impl.service.BrandServiceImpl;
import app.ecommerce.shared.impl.persistence.DatabaseConstraintInspector;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class BrandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final BrandRepository repository = mock(BrandRepository.class);
    private final BrandServiceImpl service =
        new BrandServiceImpl(
            repository,
            new BrandMapper(),
            CLOCK,
            new DatabaseConstraintInspector()
        );

    @Test
    void createsBrandWithNormalizedNameAndServerManagedFields() {
        final var brandId = UUID.randomUUID();
        when(repository.existsByNameIgnoringCase("Apple", null)).thenReturn(false);
        when(repository.saveAndFlush(any(BrandEntity.class))).thenAnswer(invocation -> {
            final BrandEntity entity = invocation.getArgument(0);
            entity.setBrandId(brandId);
            return entity;
        });

        final var response = service.createBrand(new CreateBrandRequest("  Apple  "));

        assertThat(response.brandId()).isEqualTo(brandId);
        assertThat(response.brandName()).isEqualTo("Apple");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsBrandWhenNameAlreadyExists() {
        when(repository.existsByNameIgnoringCase("Apple", null)).thenReturn(true);

        assertThatThrownBy(() -> service.createBrand(new CreateBrandRequest(" Apple ")))
            .isInstanceOf(BrandAlreadyExistsException.class)
            .hasMessage("Brand 'Apple' already exists");

        verify(repository, never()).saveAndFlush(any(BrandEntity.class));
    }

    @Test
    void translatesConcurrentUniqueConstraintViolationToConflict() {
        when(repository.existsByNameIgnoringCase("Apple", null)).thenReturn(false);
        final var constraintViolation = new ConstraintViolationException(
            "duplicate brand name", new SQLException(), "uq_brand_name");
        final var databaseException =
            new DataIntegrityViolationException("duplicate brand name", constraintViolation);
        when(repository.saveAndFlush(any(BrandEntity.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.createBrand(new CreateBrandRequest("Apple")))
            .isInstanceOf(BrandAlreadyExistsException.class)
            .hasMessage("Brand 'Apple' already exists")
            .hasCause(databaseException);
    }

    @Test
    void getsActiveBrandDetail() {
        final var brandId = UUID.randomUUID();
        final var entity = brand(brandId, "Apple");
        when(repository.findByBrandIdAndIsActiveTrue(brandId)).thenReturn(Optional.of(entity));

        final var response = service.getBrand(brandId);

        assertThat(response.brandId()).isEqualTo(brandId);
        assertThat(response.brandName()).isEqualTo("Apple");
    }

    @Test
    void rejectsDetailWhenBrandMissingOrInactive() {
        final var brandId = UUID.randomUUID();
        when(repository.findByBrandIdAndIsActiveTrue(brandId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBrand(brandId))
            .isInstanceOf(BrandNotFoundException.class);
    }

    @Test
    void renamesBrandCheckingUniqueness() {
        final var brandId = UUID.randomUUID();
        final var entity = brand(brandId, "Apple");
        when(repository.findByBrandIdAndIsActiveTrue(brandId)).thenReturn(Optional.of(entity));
        when(repository.existsByNameIgnoringCase("Apple Inc", brandId)).thenReturn(false);
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        final var response = service.renameBrand(brandId, new RenameBrandRequest("  Apple Inc  "));

        assertThat(response.brandName()).isEqualTo("Apple Inc");
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsRenameWhenNameBelongsToAnotherBrand() {
        final var brandId = UUID.randomUUID();
        final var entity = brand(brandId, "Apple");
        when(repository.findByBrandIdAndIsActiveTrue(brandId)).thenReturn(Optional.of(entity));
        when(repository.existsByNameIgnoringCase("Samsung", brandId)).thenReturn(true);

        assertThatThrownBy(() -> service.renameBrand(brandId, new RenameBrandRequest("Samsung")))
            .isInstanceOf(BrandAlreadyExistsException.class);

        verify(repository, never()).saveAndFlush(any(BrandEntity.class));
    }

    @Test
    void rejectsRenameWhenBrandDoesNotExist() {
        final var brandId = UUID.randomUUID();
        when(repository.findByBrandIdAndIsActiveTrue(brandId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renameBrand(brandId, new RenameBrandRequest("Apple")))
            .isInstanceOf(BrandNotFoundException.class);

        verify(repository, never()).saveAndFlush(any(BrandEntity.class));
    }

    @Test
    void deactivatesBrandAndUpdatesTimestamp() {
        final var brandId = UUID.randomUUID();
        final var entity = brand(brandId, "Apple");
        when(repository.findById(brandId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        service.deactivateBrand(brandId);

        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void treatsAlreadyInactiveBrandAsSuccessfulDeactivation() {
        final var brandId = UUID.randomUUID();
        final var entity = brand(brandId, "Apple");
        entity.setIsActive(false);
        when(repository.findById(brandId)).thenReturn(Optional.of(entity));

        service.deactivateBrand(brandId);

        verify(repository, never()).saveAndFlush(any(BrandEntity.class));
    }

    @Test
    void rejectsDeactivationWhenBrandDoesNotExist() {
        final var brandId = UUID.randomUUID();
        when(repository.findById(brandId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateBrand(brandId))
            .isInstanceOf(BrandNotFoundException.class);

        verify(repository, never()).saveAndFlush(any(BrandEntity.class));
    }

    private BrandEntity brand(final UUID brandId, final String name) {
        final var entity = new BrandEntity();
        entity.setBrandId(brandId);
        entity.setBrandName(name);
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }
}
