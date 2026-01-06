package com.natk.natk_api.departmentStorage.repository;

import com.natk.natk_api.baseStorage.intarfece.FolderAncestryRepository;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentFolderRepository extends JpaRepository<DepartmentFolderEntity, UUID>, FolderAncestryRepository {
    Optional<DepartmentFolderEntity> findByIdAndDepartmentAndIsDeletedFalse(UUID id, DepartmentEntity department);
    Optional<DepartmentFolderEntity> findByIdAndDepartmentAndIsDeletedTrue(UUID id, DepartmentEntity department);

    List<DepartmentFolderEntity> findByDepartmentAndParentFolderAndIsDeletedFalse(DepartmentEntity dept, DepartmentFolderEntity parent);
    List<DepartmentFolderEntity> findByDepartmentAndIsDeletedFalse(DepartmentEntity dept);

    boolean existsByDepartmentAndParentFolderAndNameAndIsDeletedFalse(DepartmentEntity dept, DepartmentFolderEntity parent, String name);
    boolean existsByDepartmentAndParentFolderIsNullAndNameAndIsDeletedFalse(DepartmentEntity dept, String name);

    List<DepartmentFolderEntity> findByDepartmentAndParentFolderIsNullAndIsDeletedFalse(DepartmentEntity dept);
    List<DepartmentFolderEntity> findByDepartmentAndIsDeletedTrueOrderByDeletedAtDesc(DepartmentEntity department);

    @Query(value = """
        WITH RECURSIVE parents(id, parent_folder_id) AS (
            SELECT id, parent_folder_id FROM department_folders WHERE id = :childId
            UNION ALL
            SELECT d.id, d.parent_folder_id FROM department_folders d
            JOIN parents p ON d.id = p.parent_folder_id
        )
        SELECT COUNT(1) > 0 FROM parents WHERE id = :ancestorId
        """, nativeQuery = true)
    boolean isAncestor(@Param("childId") UUID childId, @Param("ancestorId") UUID ancestorId);

    List<DepartmentFolderEntity> findByIsDeletedTrueAndDeletedAtBefore(Instant cutoff);

    List<DepartmentFolderEntity> findByDepartmentAndParentFolderAndIsDeletedTrue(
            DepartmentEntity department,
            DepartmentFolderEntity parent
    );

    List<DepartmentFolderEntity> findByDepartmentAndParentFolderIsNullAndIsDeletedTrue(
            DepartmentEntity department
    );
}