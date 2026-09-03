package app.ecommerce.security.impl.config;

import app.ecommerce.shared.impl.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SecurityProblemDetailWriter {

    private final ObjectMapper objectMapper;

    public void write(//
                      final HttpServletResponse response, //
                      final HttpStatus status, //
                      final String code, //
                      final String detail //
    ) throws IOException {
        final var body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty("code", code);
        body.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}