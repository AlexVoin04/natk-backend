package com.natk.natk_api.departmentStorage.repository;

import com.natk.natk_api.departmentStorage.model.DepartmentFolderAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartmentFolderAccessRepository extends JpaRepository<DepartmentFolderAccessEntity, UUID> {
    boolean existsByUserIdAndFolderId(UUID userId, UUID folderId);
}
