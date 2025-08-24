package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.baseStorage.intarfece.FolderNameResolver;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFolderNameResolverService implements FolderNameResolver<UserFolderEntity, UserEntity> {

    private final UserFolderRepository folderRepo;

    @Override
    public void ensureUniqueNameOrThrow(String desiredName, UserFolderEntity parentFolder, UserEntity user) {
        boolean exists = (parentFolder == null)
                ? folderRepo.existsByUserAndParentFolderIsNullAndNameAndIsDeletedFalse(user, desiredName)
                : folderRepo.existsByUserAndParentFolderAndNameAndIsDeletedFalse(user, parentFolder, desiredName);

        if (exists) {
            throw new IllegalArgumentException("Folder with the same name already exists in this directory");
        }
    }
}
