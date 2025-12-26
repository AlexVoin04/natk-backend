package com.natk.natk_api.audit.model;

import com.natk.natk_api.audit.enums.PurgeAuditType;
import com.natk.natk_api.audit.enums.StorageScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/*
/ Audit должен быть устойчив к удалению пользователей и организаций
 */
@Entity
@Table(name = "storage_purge_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoragePurgeAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "department_id")
    private UUID departmentId; // nullable

    @Column(name = "target_id")
    private UUID targetId;     // file or folder id

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "target_type", nullable = false, columnDefinition = "audit_file_type")
    private PurgeAuditType targetType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "storage", nullable = false, columnDefinition = "audit_file_storage")
    private StorageScope storage;    // USER | DEPARTMENT

    @Column(name = "purged_at")
    private Instant purgedAt;

    @Column(name = "files_deleted")
    private int filesDeleted;  // сколько реальных файлов удалено

    @Column(name = "folders_deleted")
    private int foldersDeleted;
}
