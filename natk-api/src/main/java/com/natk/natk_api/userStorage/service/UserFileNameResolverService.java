package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.baseStorage.service.AbstractFileNameResolverService;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
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
public class UserFileNameResolverService extends AbstractFileNameResolverService<UserFolderEntity, UserEntity> {

    private final UserFileRepository fileRepo;

    @Override
    protected Set<String> getExistingFileNames(UserFolderEntity parentFolder, UserEntity user, UUID excludeFileId) {
        List<UserFileEntity> files;
        if (parentFolder == null) {
            files = fileRepo.findByCreatedByAndFolderIsNullAndIsDeletedFalse(user);
        } else {
            files = fileRepo.findByFolderAndIsDeletedFalse(parentFolder);
        }

        return files.stream()
                .filter(f -> !Objects.equals(f.getId(), excludeFileId))
                .map(UserFileEntity::getName)
                .collect(Collectors.toSet());
    }
}
