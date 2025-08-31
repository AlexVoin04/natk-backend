package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.baseStorage.service.AbstractFolderNameResolverService;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserFolderNameResolverService
        extends AbstractFolderNameResolverService<UserFolderEntity, UserEntity> {

    private final UserFolderRepository folderRepo;

    @Override
    protected Set<String> getExistingFolderNames(UserFolderEntity parentFolder, UserEntity user, UUID excludeFolderId) {
        List<UserFolderEntity> folders =
                parentFolder == null
                        ? folderRepo.findByUserAndParentFolderIsNullAndIsDeletedFalse(user)
                        : folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, parentFolder);

        return folders.stream()
                .filter(f -> !Objects.equals(f.getId(), excludeFolderId))
                .map(UserFolderEntity::getName)
                .collect(Collectors.toSet());
    }
}
