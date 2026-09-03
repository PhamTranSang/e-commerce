package app.ecommerce.security.impl.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityProblemDetailWriter writer;

    @Override
    public void handle(//
                       final @NonNull HttpServletRequest request, //
                       final @NonNull HttpServletResponse response, //
                       final @NonNull AccessDeniedException accessDeniedException //
    ) throws IOException {
        writer.write(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to access this resource.");
    }
}