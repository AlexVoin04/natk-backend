package com.natk.natk_api.departmentStorage.repository;

import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentFileRepository extends JpaRepository<DepartmentFileEntity, UUID> {

    Optional<DepartmentFileEntity> findByIdAndDepartmentAndIsDeletedFalse(UUID id, DepartmentEntity department);

    Optional<DepartmentFileEntity> findByIdAndDepartmentAndIsDeletedTrue(UUID id, DepartmentEntity department);

    List<DepartmentFileEntity> findByDepartmentAndFolderAndIsDeletedFalse(DepartmentEntity department, DepartmentFolderEntity folder);

    List<DepartmentFileEntity> findByDepartmentAndIsDeletedFalse(DepartmentEntity department);

    boolean existsByDepartmentAndFolderAndNameAndIsDeletedFalse(DepartmentEntity department, DepartmentFileEntity folder, String name);

    List<DepartmentFileEntity> findByDepartmentAndFolderIsNullAndIsDeletedFalse(DepartmentEntity department);

    List<DepartmentFileEntity> findByDepartmentAndIsDeletedTrueOrderByDeletedAtDesc(DepartmentEntity department);
}
