package com.natk.natk_api.departmentStorage.model;

import com.natk.natk_api.department.model.DepartmentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "department_folders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentFolderEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "parent_folder_id")
    private DepartmentFolderEntity parentFolder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    @ManyToOne(optional = false)
    private String createdBy;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

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
        DepartmentFolderEntity current = this;

        while (current != null) {
            pathSegments.add(current.getName());
            current = current.getParentFolder();
        }

        Collections.reverse(pathSegments);
        return "Департамент/" + department.getName() + "/" + String.join("/", pathSegments);
    }
}