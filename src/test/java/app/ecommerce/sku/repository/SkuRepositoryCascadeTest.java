package app.ecommerce.sku.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.ecommerce.catalog.impl.entity.CategoryEntity;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.sku.impl.entity.SkuEntity;
import app.ecommerce.sku.impl.repository.SkuRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Exercises the {@code @Modifying} bulk cascade queries against a real (H2) database
 * so their JPQL — including the two-level {@code s.product.category.categoryId} path —
 * is actually translated and executed, which mock-based unit tests cannot verify.
 */
@DataJpaTest(properties = {
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SkuRepositoryCascadeTest {

    private static final Instant NOW = Instant.parse("2026-08-31T10:00:00Z");
    private static final Instant CASCADE_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SkuRepository repository;

    @Test
    void deactivateAllByProductIdFlipsOnlySkusOfThatProduct() {
        final var category = persistCategory("Electronics");
        final var target = persistProduct(category, "Laptop");
        final var other = persistProduct(category, "Mouse");
        final var targetSku = persistSku(target, "SKU-TARGET");
        final var otherSku = persistSku(other, "SKU-OTHER");
        em.flush();

        final var affected = repository.deactivateAllByProductId(target.getProductId(), CASCADE_AT);
        em.clear();

        assertThat(affected).isEqualTo(1);
        assertThat(reload(targetSku).getIsActive()).isFalse();
        assertThat(reload(targetSku).getUpdatedAt()).isEqualTo(CASCADE_AT);
        assertThat(reload(otherSku).getIsActive()).isTrue();
    }

    @Test
    void deactivateAllByCategoryIdFlipsSkusAcrossAllProductsOfThatCategory() {
        final var target = persistCategory("Electronics");
        final var other = persistCategory("Books");
        final var targetProductA = persistProduct(target, "Laptop");
        final var targetProductB = persistProduct(target, "Mouse");
        final var otherProduct = persistProduct(other, "Novel");
        final var skuA = persistSku(targetProductA, "SKU-A");
        final var skuB = persistSku(targetProductB, "SKU-B");
        final var skuOther = persistSku(otherProduct, "SKU-OTHER");
        em.flush();

        final var affected =
            repository.deactivateAllByCategoryId(target.getCategoryId(), CASCADE_AT);
        em.clear();

        assertThat(affected).isEqualTo(2);
        assertThat(reload(skuA).getIsActive()).isFalse();
        assertThat(reload(skuB).getIsActive()).isFalse();
        assertThat(reload(skuOther).getIsActive()).isTrue();
    }

    @Test
    void deactivateAllByCategoryIdSkipsAlreadyInactiveSkus() {
        final var category = persistCategory("Electronics");
        final var product = persistProduct(category, "Laptop");
        final var alreadyInactive = persistSku(product, "SKU-OLD");
        alreadyInactive.setIsActive(false);
        em.persist(alreadyInactive);
        em.flush();

        final var affected =
            repository.deactivateAllByCategoryId(category.getCategoryId(), CASCADE_AT);

        assertThat(affected).isZero();
    }

    private CategoryEntity persistCategory(final String name) {
        final var entity = new CategoryEntity();
        entity.setCategoryName(name);
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return em.persist(entity);
    }

    private ProductEntity persistProduct(final CategoryEntity category, final String name) {
        final var entity = new ProductEntity();
        entity.setCategory(category);
        entity.setProductName(name);
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return em.persist(entity);
    }

    private SkuEntity persistSku(final ProductEntity product, final String skuCode) {
        final var entity = new SkuEntity();
        entity.setProduct(product);
        entity.setSkuCode(skuCode);
        entity.setWeightGrams(500);
        entity.setAmount(new BigDecimal("19.99"));
        entity.setCurrency("USD");
        entity.setIsActive(true);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return em.persist(entity);
    }

    private SkuEntity reload(final SkuEntity sku) {
        return repository.findById(sku.getSkuId()).orElseThrow();
    }
}