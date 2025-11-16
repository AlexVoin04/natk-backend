package com.natk.natk_api.userStorage.model;

import com.natk.natk_api.users.model.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "user_files")
public class UserFileEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = true)
    @JoinColumn(name = "folder_id", nullable = true)
    private UserFolderEntity folder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "file_type")
    private String fileType;

    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "file_data", columnDefinition = "BYTEA", nullable = false)
    private byte[] fileData;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}