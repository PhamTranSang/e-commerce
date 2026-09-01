package app.ecommerce.brand.impl.entity;

import app.ecommerce.shared.impl.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "t_brand")
public class BrandEntity extends AuditableEntity {

    @Id
    @Column(name = "brand_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID brandId;

    @NotNull
    @Size(max = 255)
    @Column(name = "brand_name", nullable = false)
    private String brandName;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
