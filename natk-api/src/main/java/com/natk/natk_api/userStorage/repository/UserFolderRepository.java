package com.natk.natk_api.userStorage.repository;

import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.users.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFolderRepository extends JpaRepository<UserFolderEntity, UUID> {
    Optional<UserFolderEntity> findByIdAndUser(UUID id, UserEntity user);
    Optional<UserFolderEntity> findByIdAndUserAndIsDeletedFalse(UUID id, UserEntity user);
    List<UserFolderEntity> findByUserAndParentFolderAndIsDeletedFalse(UserEntity user, UserFolderEntity parent);
    List<UserFolderEntity> findByUserAndIsDeletedTrueOrderByDeletedAtDesc(UserEntity user);
    List<UserFolderEntity> findByUserAndIsDeletedFalse(UserEntity user);
}
