package com.natk.natk_api.userStorage.context;

import com.natk.natk_api.baseStorage.intarfece.DeletionContext;
import com.natk.natk_api.userStorage.service.UserBaseFileService;
import com.natk.natk_api.userStorage.service.UserBaseFolderService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserDeletionContext implements DeletionContext {

    private final UserBaseFileService fileService;
    private final UserBaseFolderService folderService;

    public UserDeletionContext(UserBaseFileService fileService, UserBaseFolderService folderService) {
        this.fileService = fileService;
        this.folderService = folderService;
    }

    @Override
    public void deleteFile(UUID fileId) {
        fileService.deleteFile(fileId);
    }

    @Override
    public void deleteFolder(UUID folderId) {
        folderService.deleteFolder(folderId);
    }
}
