package app.ecommerce.sku.impl.entity;

import app.ecommerce.product.impl.entity.ProductEntity;
import app.ecommerce.product.impl.entity.ProductOptionValueEntity;
import app.ecommerce.shared.impl.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "t_sku")
public class SkuEntity extends AuditableEntity {

    @Id
    @Column(name = "sku_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID skuId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @BatchSize(size = 50)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "t_sku_option_value",
        joinColumns = @JoinColumn(name = "sku_id"),
        inverseJoinColumns = @JoinColumn(name = "option_value_id")
    )
    private Set<ProductOptionValueEntity> optionValues = new HashSet<>();

    @NotNull
    @Size(max = 64)
    @Column(name = "sku_code", nullable = false, length = 64)
    private String skuCode;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @Positive
    @Column(name = "weight_grams", nullable = false)
    private Integer weightGrams;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
