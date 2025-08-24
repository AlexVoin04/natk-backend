package com.natk.natk_api.departmentStorage.repository;

import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentFolderRepository extends JpaRepository<DepartmentFolderEntity, UUID> {
    Optional<DepartmentFolderEntity> findByIdAndDepartmentAndIsDeletedFalse(UUID id, DepartmentEntity department);
    Optional<DepartmentFolderEntity> findByIdAndDepartmentAndIsDeletedTrue(UUID id, DepartmentEntity department);

    List<DepartmentFolderEntity> findByDepartmentAndParentFolderAndIsDeletedFalse(DepartmentEntity dept, DepartmentFolderEntity parent);
    List<DepartmentFolderEntity> findByDepartmentAndIsDeletedFalse(DepartmentEntity dept);

    boolean existsByDepartmentAndParentFolderAndNameAndIsDeletedFalse(DepartmentEntity dept, DepartmentFolderEntity parent, String name);
    boolean existsByDepartmentAndParentFolderIsNullAndNameAndIsDeletedFalse(DepartmentEntity dept, String name);
}
