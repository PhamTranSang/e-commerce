package app.ecommerce.shared.impl.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DatabaseConstraintInspectorTest {

    private final DatabaseConstraintInspector inspector = new DatabaseConstraintInspector();

    @Test
    void findsNamedConstraintInsideNestedExceptionChain() {
        final var constraintViolation = new ConstraintViolationException(
            "duplicate value",
            new SQLException(),
            "uq_category_name_normalized"
        );
        final var exception = new DataIntegrityViolationException("insert failed", constraintViolation);

        assertThat(inspector.isViolationOf(exception, "uq_category_name_normalized")).isTrue();
    }

    @Test
    void doesNotMatchDifferentConstraint() {
        final var constraintViolation = new ConstraintViolationException(
            "duplicate value",
            new SQLException(),
            "uq_category_name_normalized"
        );
        final var exception = new DataIntegrityViolationException("insert failed", constraintViolation);

        assertThat(inspector.isViolationOf(exception, "uq_sku_code")).isFalse();
    }
}
