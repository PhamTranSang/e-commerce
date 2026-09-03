package app.ecommerce.security.impl.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AccountPrincipal extends User {

    private final UUID accountId;
    private final String email;
    private final String fullName;
    private final List<String> roles;

    public AccountPrincipal(//
                            final UUID accountId, //
                            final String email, //
                            final String fullName, //
                            final String login, //
                            final String passwordHash, //
                            final List<String> roles, //
                            final boolean active, //
                            final Collection<? extends GrantedAuthority> authorities //
    ) {
        super(login, passwordHash, active, true, true, true, authorities);
        this.accountId = accountId;
        this.email = email;
        this.fullName = fullName;
        this.roles = List.copyOf(roles);
    }

    public UUID accountId() {
        return accountId;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public List<String> roles() {
        return roles;
    }
}