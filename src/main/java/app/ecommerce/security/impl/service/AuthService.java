package app.ecommerce.security.impl.service;

import app.ecommerce.security.api.dto.request.LoginRequest;
import app.ecommerce.security.api.dto.response.AuthenticatedAccountResponse;
import app.ecommerce.security.api.dto.response.LoginResponse;
import app.ecommerce.security.api.exceptions.AccountNotFoundException;
import app.ecommerce.security.api.exceptions.InvalidCredentialsException;
import app.ecommerce.security.impl.config.JwtProperties;
import app.ecommerce.security.impl.repository.AccountRepository;
import app.ecommerce.security.impl.repository.AccountRoleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public LoginResponse login(final LoginRequest request) {
        final AccountPrincipal principal;
        try {
            final var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            principal = (AccountPrincipal) authentication.getPrincipal();
            final var accountResponse = new AuthenticatedAccountResponse(
                    principal.accountId(), principal.email(), principal.fullName(), principal.roles());
            final var token = jwtService.issue(accountResponse);
            log.info("Login succeeded: accountId={}", principal.accountId());

            return new LoginResponse(
                token,
                "Bearer",
                jwtProperties.getTtl().toSeconds(),
                principal.accountId().toString(),
                principal.email(),
                principal.fullName(),
                principal.roles()
            );
        } catch (final AuthenticationException e) {
            log.debug("Login rejected: {}", e.getClass().getSimpleName());
            throw new InvalidCredentialsException();
        }
    }

    @Transactional(readOnly = true)
    public AuthenticatedAccountResponse currentAccount(final UUID accountId) {
        final var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
        final var roles = accountRoleRepository.findRolesByAccountId(accountId).stream()
            .map(item -> item.getRole().getRoleCode())
            .toList();
        return new AuthenticatedAccountResponse(
            account.getAccountId(), account.getEmail(), account.getFullName(), roles);
    }
}
