package app.ecommerce.sku.api.exceptions;

import app.ecommerce.shared.api.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Raised when the option values chosen for a SKU do not form a valid variant of the product:
 * an unknown value, a value from another product, two values of the same option, or a
 * combination that does not cover exactly one value per option.
 */
public final class InvalidVariantCombinationException extends BusinessException {

    public static final String CODE = "INVALID_VARIANT_COMBINATION";

    public InvalidVariantCombinationException(final String detail) {
        super(HttpStatus.CONFLICT, CODE, detail);
    }
}
