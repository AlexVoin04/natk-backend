package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.users.model.UserEntity;
import lombok.experimental.UtilityClass;
import org.springframework.security.access.AccessDeniedException;

@UtilityClass
public class FileSecurityService {

    public void assertFileOwnership(UserFileEntity file, UserEntity user) {
        if (!file.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("File not found or not owned by user");
        }
    }

    public void assertFolderOwnership(UserFolderEntity folder, UserEntity user) {
        if (!folder.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Folder not found or not owned by user");
        }
    }
}
