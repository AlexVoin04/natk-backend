package com.natk.natk_api.userStorage.repository;

import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.users.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserFolderRepository extends JpaRepository<UserFolderEntity, UUID> {
    List<UserFolderEntity> findByUserAndParentFolderAndIsDeletedFalse(UserEntity user, UserFolderEntity parent);
    List<UserFolderEntity> findByUserAndIsDeletedTrueOrderByDeletedAtDesc(UserEntity user);
    List<UserFolderEntity> findByUserAndIsDeletedFalse(UserEntity user);
}
