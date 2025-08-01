package com.natk.natk_api.userStorage.model;

import com.natk.natk_api.users.model.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "user_folders")
public class UserFolderEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "parent_folder_id")
    private UserFolderEntity parentFolder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public String buildPath() {
        List<String> pathSegments = new ArrayList<>();
        UserFolderEntity current = this;

        while (current != null) {
            pathSegments.add(current.getName());
            current = current.getParentFolder();
        }

        Collections.reverse(pathSegments);
        return "Все файлы/" + String.join("/", pathSegments);
    }
}
