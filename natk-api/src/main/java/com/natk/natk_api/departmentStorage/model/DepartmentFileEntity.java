package com.natk.natk_api.departmentStorage.model;

import com.natk.natk_api.baseStorage.enums.FileStatus;
import com.natk.natk_api.department.model.DepartmentEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "department_files")
@Getter
@Setter
public class DepartmentFileEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = true)
    @JoinColumn(name = "folder_id", nullable = true)
    private DepartmentFolderEntity folder;

    @Column(nullable = false)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    @Column(nullable = false)
    private Instant createdAt;

    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "file_data", columnDefinition = "BYTEA", nullable = false)
    private byte[] fileData;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "file_status")
    private FileStatus status;
}
