package com.natk.natk_api.userStorage.repository;

import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.users.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFileRepository extends JpaRepository<UserFileEntity, UUID> {
    Optional<UserFileEntity> findByIdAndCreatedBy(UUID id, UserEntity createdBy);
    Optional<UserFileEntity> findByIdAndCreatedByAndIsDeletedFalse(UUID id, UserEntity createdBy);
    List<UserFileEntity> findByFolderAndIsDeletedFalse(UserFolderEntity folder);
    List<UserFileEntity> findByCreatedByAndFolderIsNullAndIsDeletedFalse(UserEntity createdBy);
    List<UserFileEntity> findByCreatedByAndIsDeletedTrueOrderByDeletedAtDesc(UserEntity createdBy);
}
