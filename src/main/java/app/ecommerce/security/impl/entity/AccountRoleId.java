package app.ecommerce.security.impl.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @IdClass} for {@link AccountRoleEntity}. Field names match the entity's
 * {@code @Id} association fields ({@code account}, {@code role}); their types are the
 * id types of the referenced entities (UUID).
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class AccountRoleId implements Serializable {

    private UUID account;

    private UUID role;
}
