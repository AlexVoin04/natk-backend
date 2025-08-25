package com.natk.natk_api.userStorage.repository;

import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.users.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFolderRepository extends JpaRepository<UserFolderEntity, UUID> {
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
}
