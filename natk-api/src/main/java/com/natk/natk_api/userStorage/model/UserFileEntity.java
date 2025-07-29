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

    @ManyToOne(optional = false)
    @JoinColumn(name = "folder_id")
    private UserFolderEntity folder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    private Instant createdAt;

    private String fileType;

    @Lob
    @Column(name = "file_data", columnDefinition = "BYTEA")
    private byte[] fileData;
}