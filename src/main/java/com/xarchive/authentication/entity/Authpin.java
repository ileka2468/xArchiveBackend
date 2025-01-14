package com.xarchive.authentication.entity;

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
@Table(name = "authpin", schema = "public")
public class Authpin {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authpin_id_gen")
    @SequenceGenerator(name = "authpin_id_gen", sequenceName = "authpin_pin_id_seq1", allocationSize = 1)
    @Column(name = "pin_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 10)
    @NotNull
    @Column(name = "pin_code", nullable = false, length = 10)
    private String pinCode;

    @NotNull
    @Column(name = "expiry_ts", nullable = false)
    private Instant expiryTs;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Size(max = 50)
    @Column(name = "intent", nullable = false)
    private String intent; // For example: "CHANGE_EMAIL"

    @Column(name = "data")
    private String data; // Optional JSON-encoded or plain string data
}
