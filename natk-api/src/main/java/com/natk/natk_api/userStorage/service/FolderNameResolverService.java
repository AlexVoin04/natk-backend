package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FolderNameResolverService {

    private final UserFolderRepository folderRepo;

    public void ensureUniqueNameOrThrow(String desiredName, UserFolderEntity parentFolder, UserEntity user) {
        boolean exists;
        if (parentFolder == null) {
            exists = folderRepo.existsByUserAndParentFolderIsNullAndNameAndIsDeletedFalse(user, desiredName);
        } else {
            exists = folderRepo.existsByUserAndParentFolderAndNameAndIsDeletedFalse(user, parentFolder, desiredName);
        }

        if (exists) {
            throw new IllegalArgumentException("Folder with the same name already exists in this directory");
        }
    }
}
