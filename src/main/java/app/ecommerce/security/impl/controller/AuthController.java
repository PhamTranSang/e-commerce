package app.ecommerce.security.impl.controller;

import app.ecommerce.security.api.dto.request.LoginRequest;
import app.ecommerce.security.api.dto.response.AuthenticatedAccountResponse;
import app.ecommerce.security.api.dto.response.LoginResponse;
import app.ecommerce.security.impl.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody final LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthenticatedAccountResponse me(@AuthenticationPrincipal final String accountId) {
        return authService.currentAccount(UUID.fromString(accountId));
    }
}