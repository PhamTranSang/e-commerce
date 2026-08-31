package app.ecommerce.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.ecommerce.catalog.impl.entity.CategoryEntity;
import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.repository.ProductRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Exercises the product {@code @Modifying} category-cascade query against a real (H2)
 * database so its JPQL is actually translated and executed, which mock-based unit tests
 * cannot verify.
 */
@DataJpaTest(properties = {
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProductRepositoryCascadeTest {

    private static final Instant NOW = Instant.parse("2026-08-31T10:00:00Z");
    private static final Instant CASCADE_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProductRepository repository;

    @Test
    void deactivateAllByCategoryIdFlipsOnlyProductsOfThatCategory() {
        final var target = persistCategory("Electronics");
        final var other = persistCategory("Books");
        final var targetProductA = persistProduct(target, "Laptop");
        final var targetProductB = persistProduct(target, "Mouse");
        final var otherProduct = persistProduct(other, "Novel");
        em.flush();

        final var affected =
            repository.deactivateAllByCategoryId(target.getCategoryId(), CASCADE_AT);
        em.clear();

        assertThat(affected).isEqualTo(2);
        assertThat(reload(targetProductA).getIsActive()).isFalse();
        assertThat(reload(targetProductA).getUpdatedAt()).isEqualTo(CASCADE_AT);
        assertThat(reload(targetProductB).getIsActive()).isFalse();
        assertThat(reload(otherProduct).getIsActive()).isTrue();
    }

    @Test
    void deactivateAllByCategoryIdSkipsAlreadyInactiveProducts() {
        final var category = persistCategory("Electronics");
        final var alreadyInactive = persistProduct(category, "Laptop");
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

    private ProductEntity reload(final ProductEntity product) {
        return repository.findById(product.getProductId()).orElseThrow();
    }
}
