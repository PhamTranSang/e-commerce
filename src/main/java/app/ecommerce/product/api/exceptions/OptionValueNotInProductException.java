package app.ecommerce.product.api.exceptions;

import app.ecommerce.shared.api.exceptions.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Raised when an image is tied to an option value that does not belong to the product
 * (either it does not exist or it belongs to a different product).
 */
public final class OptionValueNotInProductException extends BusinessException {

    public static final String CODE = "OPTION_VALUE_NOT_IN_PRODUCT";

    public OptionValueNotInProductException(final UUID optionValueId, final UUID productId) {
        super(
            HttpStatus.CONFLICT,
            CODE,
            "Option value '%s' does not belong to product '%s'".formatted(optionValueId, productId)
        );
    }
}
