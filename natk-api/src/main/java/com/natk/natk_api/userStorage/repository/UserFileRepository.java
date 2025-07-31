package com.natk.natk_api.userStorage.repository;

import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.users.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserFileRepository extends JpaRepository<UserFileEntity, UUID> {
    List<UserFileEntity> findByFolder(UserFolderEntity folder);
    List<UserFileEntity> findByCreatedByAndFolderIsNull(UserEntity createdBy);
}
