package com.xarchive.licensing.entity;

import com.xarchive.authentication.entity.User;
import com.xarchive.billing.entity.Plan;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "licenses", schema = "public")
public class License {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 50)
    @NotNull
    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "license_type", nullable = false)
    private Plan licenseType;


    @ColumnDefault("true")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Size(max = 100)
    @Column(name = "hardware_identifier", length = 100)
    private String hardwareIdentifier;


    @ColumnDefault("false")
    @Column(name = "has_been_activated", nullable = false)
    private Boolean hasBeenActivated = false;

    @Column(name = "activation_date")
    private Instant activationDate;

    @Column(name = "expiration_date")
    private Instant expirationDate;


    @ColumnDefault("false")
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = false;

    @Size(max = 10)

    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 10)
    private String status;


    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @PrePersist
    void prePersist() {
        if (enabled == null) enabled = true;
        if (hasBeenActivated == null) hasBeenActivated = false;
        if (status == null) status = "ACTIVE";
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (autoRenew == null) autoRenew = false;
    }


}