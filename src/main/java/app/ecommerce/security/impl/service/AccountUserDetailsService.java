package app.ecommerce.security.impl.service;

import app.ecommerce.security.impl.repository.AccountRoleRepository;
import app.ecommerce.security.impl.repository.PasswordCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@NullMarked
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private final PasswordCredentialRepository credentialRepository;
    private final AccountRoleRepository accountRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String username) {
        final var credential = credentialRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        final var account = credential.getAccount();

        final var roles = accountRoleRepository.findRolesByAccountId(account.getAccountId()).stream()
            .map(item -> item.getRole().getRoleCode())
            .toList();
        final var authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        return new AccountPrincipal(
            account.getAccountId(),
            username,
            account.getEmail(),
            account.getFullName(),
            credential.getPasswordHash(),
            roles,
            Boolean.TRUE.equals(account.getIsActive()),
            authorities
        );
    }
}
