package app.ecommerce.security.api.dto.response;

import java.util.List;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    String accountId,
    String email,
    String fullName,
    List<String> roles
) {
}
