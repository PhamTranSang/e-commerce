package app.ecommerce.security.impl.service;

import app.ecommerce.security.api.dto.response.AuthenticatedAccountResponse;
import app.ecommerce.security.impl.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final Clock clock;
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(final Clock clock, final JwtProperties properties) {
        this.clock = clock;
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(final AuthenticatedAccountResponse account) {
        final var now = clock.instant();
        return Jwts.builder()
            .issuer(properties.getIssuer())
            .subject(account.accountId().toString())
            .claim("email", account.email())
            .claim("fullName", account.fullName())
            .claim("roles", account.roles())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.getTtl())))
            .signWith(key)
            .compact();
    }

     @SuppressWarnings("unchecked")
    public TokenClaims parse(final String token) {
        final Claims claims = Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.getIssuer())
            .clock(() -> Date.from(clock.instant()))
            .build()
            .parseSignedClaims(token)
            .getPayload();
        final var roles = claims.get("roles", List.class);
        return new TokenClaims(claims.getSubject(), roles == null ? List.of() : roles);
    }

    public record TokenClaims(String subject, List<String> roles) {
    }
}