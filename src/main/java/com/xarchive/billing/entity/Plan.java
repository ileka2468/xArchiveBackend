package com.xarchive.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "plans", schema = "public")
public class Plan {
    @Id
    @Column(name = "plan_id", nullable = false)
    private Integer id;

    @Size(max = 50)
    @NotNull
    @Column(name = "plan_name", nullable = false, length = 50)
    private String planName;

    @NotNull
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Size(max = 10)
    @NotNull
    @Column(name = "billing_cycle", nullable = false, length = 10)
    private String billingCycle;

    @Column(name = "features")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> features;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @NotNull
    @Column(name = "stripe_price_id", nullable = false)
    private String stripePriceId;
}