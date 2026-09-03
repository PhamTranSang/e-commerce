package app.ecommerce.security.impl.repository;

import app.ecommerce.security.impl.entity.AccountRoleEntity;
import app.ecommerce.security.impl.entity.AccountRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRoleRepository extends JpaRepository<AccountRoleEntity, AccountRoleId> {

    @Query("select ar from AccountRoleEntity ar join fetch ar.role where ar.account.accountId = :accountId")
    List<AccountRoleEntity> findRolesByAccountId(@Param("accountId") UUID accountId);
}
