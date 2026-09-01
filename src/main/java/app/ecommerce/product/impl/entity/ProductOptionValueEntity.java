package app.ecommerce.product.impl.entity;

import app.ecommerce.shared.impl.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "t_product_option_value")
public class ProductOptionValueEntity extends AuditableEntity {

    @Id
    @Column(name = "option_value_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID optionValueId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOptionEntity option;

    @NotNull
    @Size(max = 255)
    @Column(name = "value", nullable = false)
    private String value;

    @NotNull
    @Column(name = "position", nullable = false)
    private Integer position;
}
