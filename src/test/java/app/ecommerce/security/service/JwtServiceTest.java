package app.ecommerce.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.ecommerce.security.api.dto.response.AuthenticatedAccountResponse;
import app.ecommerce.security.impl.config.JwtProperties;
import app.ecommerce.security.impl.service.JwtService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID ACCOUNT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private JwtService serviceWithSecret(final String secret) {
        final var properties = new JwtProperties(secret, "e-commerce", Duration.ofHours(12));
        return new JwtService(CLOCK, properties);
    }

    private AuthenticatedAccountResponse account() {
        return new AuthenticatedAccountResponse(
            ACCOUNT_ID, "admin@ecommerce.local", "Admin User", List.of("ADMIN", "STAFF"));
    }

    @Test
    void issuesAndVerifiesTokenClaims() {
        final var service = serviceWithSecret("0123456789012345678901234567890123");

        final var token = service.issue(account());
        final var claims = service.parse(token);

        assertThat(claims.subject()).isEqualTo(ACCOUNT_ID.toString());
        assertThat(claims.roles()).containsExactly("ADMIN", "STAFF");
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        final var issuer = serviceWithSecret("0123456789012345678901234567890123");
        final var attacker = serviceWithSecret("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        final var token = attacker.issue(account());

        assertThatThrownBy(() -> issuer.parse(token)).isInstanceOf(Exception.class);
    }

    @Test
    void rejectsExpiredToken() {
        final var issuer = serviceWithSecret("0123456789012345678901234567890123");
        final var token = issuer.issue(account());

        final var later = new JwtServiceAtInstant(NOW.plus(Duration.ofHours(13)));
        assertThatThrownBy(() -> later.service().parse(token)).isInstanceOf(Exception.class);
    }

    /** Helper: the same signing secret but a clock advanced past the token's expiry. */
    private static final class JwtServiceAtInstant {
        private final JwtService service;

        JwtServiceAtInstant(final Instant instant) {
            final var properties = new JwtProperties(
                "0123456789012345678901234567890123", "e-commerce", Duration.ofHours(12));
            this.service = new JwtService(Clock.fixed(instant, ZoneOffset.UTC), properties);
        }

        JwtService service() {
            return service;
        }
    }
}
