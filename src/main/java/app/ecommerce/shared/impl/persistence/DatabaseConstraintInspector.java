package app.ecommerce.shared.impl.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConstraintInspector {

    public boolean isViolationOf(final Throwable exception, final String constraintName) {
        var cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                && constraintName.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }
}