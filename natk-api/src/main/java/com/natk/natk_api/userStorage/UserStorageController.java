package com.natk.natk_api.userStorage;

import com.natk.natk_api.userStorage.dto.*;
import com.natk.natk_api.userStorage.service.UserFileService;
import com.natk.natk_api.userStorage.service.UserFolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class UserStorageController {
    private final UserFileService userFileService;
    private final UserFolderService userFolderService;

    @PostMapping("/folders")
    public FolderDto createFolder(@RequestBody CreateFolderDto dto) {
        var folder = userFolderService.createFolder(dto);
        return new FolderDto(folder.getId(), folder.getName());
    }

    @PostMapping("/files")
    public FileInfoDto uploadFile(@RequestBody UploadFileDto dto) {
        var file = userFileService.uploadFile(dto);
        return new FileInfoDto(file.getId(), file.getName(), file.getFileType(), file.getCreatedAt());
    }

    @GetMapping("/files/{id}")
    public FileInfoDto getFileInfo(@PathVariable UUID id) {
        return userFileService.getFile(id);
    }

    @DeleteMapping("/files/{id}")
    public void deleteFile(@PathVariable UUID id) {
        userFileService.deleteFile(id);
    }

    @DeleteMapping("/folders/{id}")
    public void deleteFolder(@PathVariable UUID id) {
        userFolderService.deleteFolder(id);
    }

    @GetMapping("/folders")
    public List<FolderDto> listFolders(@RequestParam(required = false) UUID parentId) {
        return userFolderService.listFolders(parentId);
    }

    @GetMapping("/files")
    public List<FileInfoDto> listFiles(@RequestParam UUID folderId) {
        return userFileService.listFiles(folderId);
    }

    @PutMapping("/folders/{id}")
    public void updateFolder(@PathVariable UUID id, @RequestBody UpdateFolderDto dto) {
        userFolderService.updateFolder(id, dto);
    }

    @PutMapping("/files/{id}")
    public void updateFile(@PathVariable UUID id, @RequestBody UpdateFileDto dto) {
        userFileService.updateFile(id, dto);
    }
}
