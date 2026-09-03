package app.ecommerce.security.impl.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    @NotBlank(message = "app.security.jwt.secret must be provided")
    @Size(min = 32, message = "app.security.jwt.secret must be at least 32 characters")
    private final String secret;

    @NotBlank
    private final String issuer;

    @NotNull
    private final Duration ttl; // Time To Life

    public JwtProperties(//
                         final String secret, //
                         @DefaultValue("e-commerce") final String issuer, //
                         @DefaultValue("PT12H") final Duration ttl //
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.ttl = ttl;
    }
}