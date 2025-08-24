package com.natk.natk_api.departmentStorage.service;

import com.natk.natk_api.baseStorage.intarfece.FolderNameResolver;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentFolderNameResolverService implements FolderNameResolver<DepartmentFolderEntity, DepartmentEntity> {
    private final DepartmentFolderRepository folderRepo;

    @Override
    public void ensureUniqueNameOrThrow(String desiredName, DepartmentFolderEntity parentFolder, DepartmentEntity department) {
        boolean exists = (parentFolder == null)
                ? folderRepo.existsByDepartmentAndParentFolderIsNullAndNameAndIsDeletedFalse(department, desiredName)
                : folderRepo.existsByDepartmentAndParentFolderAndNameAndIsDeletedFalse(department, parentFolder, desiredName);

        if (exists) {
            throw new IllegalArgumentException("Folder with the same name already exists in this directory");
        }
    }
}
