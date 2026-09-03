package app.ecommerce.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.ecommerce.security.api.dto.request.LoginRequest;
import app.ecommerce.security.api.exceptions.AccountNotFoundException;
import app.ecommerce.security.api.exceptions.InvalidCredentialsException;
import app.ecommerce.security.impl.config.JwtProperties;
import app.ecommerce.security.impl.entity.AccountEntity;
import app.ecommerce.security.impl.entity.AccountRoleEntity;
import app.ecommerce.security.impl.entity.RoleEntity;
import app.ecommerce.security.impl.repository.AccountRepository;
import app.ecommerce.security.impl.repository.AccountRoleRepository;
import app.ecommerce.security.impl.service.AccountPrincipal;
import app.ecommerce.security.impl.service.AuthService;
import app.ecommerce.security.impl.service.JwtService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuthServiceTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountRoleRepository accountRoleRepository = mock(AccountRoleRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final JwtProperties jwtProperties = jwtProperties();
    private final AuthService service = new AuthService(
        authenticationManager, accountRepository, accountRoleRepository, jwtService, jwtProperties);

    @Test
    void logsInAndReturnsTokenPayload() {
        final var principal = new AccountPrincipal(
            ACCOUNT_ID, "admin@ecommerce.local", "Admin User", "admin@ecommerce.local",
            "{bcrypt}hash", List.of("ADMIN"), true,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        final var authentication = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.issue(any())).thenReturn("the-token");

        final var response = service.login(
            new LoginRequest("admin@ecommerce.local", "Admin@12345"));

        assertThat(response.accessToken()).isEqualTo("the-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(43200);
        assertThat(response.accountId()).isEqualTo(ACCOUNT_ID.toString());
        assertThat(response.email()).isEqualTo("admin@ecommerce.local");
        assertThat(response.roles()).containsExactly("ADMIN");
    }

    @Test
    void rejectsInvalidCredentials() {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login(
            new LoginRequest("admin@ecommerce.local", "wrong")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void returnsCurrentAccountWithRoles() {
        final var account = account();
        final var role = role("ADMIN");
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRoleRepository.findRolesByAccountId(ACCOUNT_ID))
            .thenReturn(List.of(accountRole(account, role)));

        final var response = service.currentAccount(ACCOUNT_ID);

        assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.roles()).containsExactly("ADMIN");
    }

    @Test
    void rejectsCurrentAccountWhenMissing() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.currentAccount(ACCOUNT_ID))
            .isInstanceOf(AccountNotFoundException.class);
    }

    private JwtProperties jwtProperties() {
        return new JwtProperties("0123456789012345678901234567890123", "e-commerce",
            Duration.ofHours(12));
    }

    private AccountEntity account() {
        final var account = new AccountEntity();
        account.setAccountId(ACCOUNT_ID);
        account.setEmail("admin@ecommerce.local");
        account.setFullName("Admin User");
        account.setIsActive(true);
        return account;
    }

    private RoleEntity role(final String code) {
        final var role = new RoleEntity();
        role.setRoleId(UUID.randomUUID());
        role.setRoleCode(code);
        return role;
    }

    private AccountRoleEntity accountRole(final AccountEntity account, final RoleEntity role) {
        final var entity = new AccountRoleEntity();
        entity.setAccount(account);
        entity.setRole(role);
        return entity;
    }
}
