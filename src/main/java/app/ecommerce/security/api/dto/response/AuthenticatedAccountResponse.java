package app.ecommerce.security.api.dto.response;

import java.util.List;
import java.util.UUID;

public record AuthenticatedAccountResponse(
    UUID accountId,
    String email,
    String fullName,
    List<String> roles
) {
}
