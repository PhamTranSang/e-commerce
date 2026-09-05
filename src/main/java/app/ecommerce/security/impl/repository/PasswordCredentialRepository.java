package app.ecommerce.security.impl.repository;

import app.ecommerce.security.impl.entity.PasswordCredentialEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordCredentialRepository extends JpaRepository<PasswordCredentialEntity, UUID> {

    Optional<PasswordCredentialEntity> findByUsernameIgnoreCase(String login);
}