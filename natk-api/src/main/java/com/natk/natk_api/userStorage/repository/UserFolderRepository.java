package com.natk.natk_api.userStorage.repository;

import com.natk.natk_api.baseStorage.intarfece.FolderAncestryRepository;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.users.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFolderRepository extends JpaRepository<UserFolderEntity, UUID>, FolderAncestryRepository {
    Optional<UserFolderEntity> findByIdAndUser(UUID id, UserEntity user);
    Optional<UserFolderEntity> findByIdAndUserAndIsDeletedFalse(UUID id, UserEntity user);
    Optional<UserFolderEntity> findByIdAndUserAndIsDeletedTrue(UUID id, UserEntity user);
    List<UserFolderEntity> findByUserAndParentFolderAndIsDeletedFalse(UserEntity user, UserFolderEntity parent);
    List<UserFolderEntity> findByUserAndIsDeletedTrueOrderByDeletedAtDesc(UserEntity user);
    List<UserFolderEntity> findByUserAndIsDeletedFalse(UserEntity user);
    List<UserFolderEntity> findByUserAndParentFolderAndIsDeletedTrue(UserEntity user, UserFolderEntity parentFolder);
    boolean existsByUserAndParentFolderAndNameAndIsDeletedFalse(UserEntity user, UserFolderEntity parentFolder, String name);
    boolean existsByUserAndParentFolderIsNullAndNameAndIsDeletedFalse(UserEntity user, String name);
    List<UserFolderEntity> findByIsDeletedTrueAndDeletedAtBefore(Instant cutoff);

    List<UserFolderEntity> findByUserAndParentFolderIsNullAndIsDeletedFalse(UserEntity user);
    List<UserFolderEntity> findByUserAndParentFolderIsNullAndIsDeletedTrue(UserEntity user);

    @Query(value = """
        WITH RECURSIVE parents(id, parent_folder_id) AS (
            SELECT id, parent_folder_id FROM user_folders WHERE id = :childId
            UNION ALL
            SELECT d.id, d.parent_folder_id FROM user_folders d
            JOIN parents p ON d.id = p.parent_folder_id
        )
        SELECT COUNT(1) > 0 FROM parents WHERE id = :ancestorId
        """, nativeQuery = true)
    boolean isAncestor(@Param("childId") UUID childId, @Param("ancestorId") UUID ancestorId);
}
