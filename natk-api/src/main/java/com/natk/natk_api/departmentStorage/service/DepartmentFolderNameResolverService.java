package com.natk.natk_api.departmentStorage.service;

import com.natk.natk_api.baseStorage.service.AbstractFolderNameResolverService;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentFolderNameResolverService
        extends AbstractFolderNameResolverService<DepartmentFolderEntity, DepartmentEntity> {

    private final DepartmentFolderRepository folderRepo;

    @Override
    protected Set<String> getExistingFolderNames(DepartmentFolderEntity parentFolder, DepartmentEntity dept) {
        List<DepartmentFolderEntity> folders;
        if (parentFolder == null) {
            folders = folderRepo.findByDepartmentAndParentFolderIsNullAndIsDeletedFalse(dept);
        } else {
            folders = folderRepo.findByDepartmentAndParentFolderAndIsDeletedFalse(dept, parentFolder);
        }
        return folders.stream()
                .map(DepartmentFolderEntity::getName)
                .collect(Collectors.toSet());
    }
}
