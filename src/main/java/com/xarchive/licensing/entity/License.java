package com.xarchive.licensing.entity;

import com.xarchive.authentication.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "licenses", schema = "public")
public class License {
    @Id
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

    @Size(max = 10)
    @NotNull
    @Column(name = "license_type", nullable = false, length = 10)
    private String licenseType;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Size(max = 100)
    @Column(name = "hardware_identifier", length = 100)
    private String hardwareIdentifier;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "has_been_activated", nullable = false)
    private Boolean hasBeenActivated = false;

    @Column(name = "activation_date")
    private Instant activationDate;

    @Column(name = "expiration_date")
    private Instant expirationDate;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = false;

    @Size(max = 10)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}