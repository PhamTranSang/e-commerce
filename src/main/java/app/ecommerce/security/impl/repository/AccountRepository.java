package app.ecommerce.security.impl.repository;

import app.ecommerce.security.impl.entity.AccountEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    @Query("select a from AccountEntity a where lower(a.email) = lower(:email) and a.isActive = true")
    Optional<AccountEntity> findActiveByEmail(@Param("email") String email);
}
