package app.ecommerce.shared.impl.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    private static final int MAX_LEN = 64;

    @Override
    protected void doFilterInternal(
        final HttpServletRequest request,
        @NonNull final HttpServletResponse response,
        @NonNull final FilterChain filterChain)
    throws ServletException, IOException {
        var id = sanitize(request.getHeader(HEADER));

        if (Objects.isNull(id)) {
            id = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String sanitize(final String raw) {
        if (Objects.isNull(raw) || raw.isBlank()) {
            return null;
        }

        final var trimmed = raw.trim();
        if (trimmed.length() > MAX_LEN) {
            return null;
        }

        return trimmed.matches("[A-Za-z0-9._-]+") ? trimmed : null;
    }
}