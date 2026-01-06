package com.natk.natk_api.departmentStorage.context;

import com.natk.natk_api.baseStorage.intarfece.DeletionContext;
import com.natk.natk_api.departmentStorage.service.DepartmentBaseFileService;
import com.natk.natk_api.departmentStorage.service.DepartmentBaseFolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DepartmentDeletionContextFactory {

    private final DepartmentBaseFileService fileService;
    private final DepartmentBaseFolderService folderService;

    public DeletionContext forDepartment(UUID departmentId) {
        return new DeletionContext() {
            @Override
            public void deleteFile(UUID fileId) {
                fileService.deleteFile(fileId, departmentId);
            }

            @Override
            public void deleteFolder(UUID folderId) {
                folderService.deleteFolder(departmentId, folderId);
            }
        };
    }
}
