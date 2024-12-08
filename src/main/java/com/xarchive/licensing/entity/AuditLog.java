package com.xarchive.licensing.entity;

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
@Table(name = "audit_logs", schema = "public")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_logs_id_gen")
    @SequenceGenerator(name = "audit_logs_id_gen", sequenceName = "audit_logs_log_id_seq1", allocationSize = 1)
    @Column(name = "log_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "license_id")
    private License license;

    @Size(max = 50)
    @NotNull
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "performed_by")
    private Integer performedBy;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "remarks", length = Integer.MAX_VALUE)
    private String remarks;

}