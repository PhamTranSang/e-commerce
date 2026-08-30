package app.ecommerce.shared.api.dto.response;

public record ValidationError(
    String field,
    String message
) {
}
