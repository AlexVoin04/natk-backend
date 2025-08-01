package com.natk.natk_api.userStorage;

import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.DeletedItemDto;
import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.dto.UpdateFileDto;
import com.natk.natk_api.userStorage.dto.UpdateFolderDto;
import com.natk.natk_api.userStorage.dto.UploadFileDto;
import com.natk.natk_api.userStorage.mapper.UserFileMapper;
import com.natk.natk_api.userStorage.mapper.UserFolderMapper;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.service.UserFileService;
import com.natk.natk_api.userStorage.service.UserFolderService;
import com.natk.natk_api.userStorage.service.UserStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class UserStorageController {
    private final UserFileService userFileService;
    private final UserFolderService userFolderService;
    private final UserStorageService userStorageService;
    private final UserFileMapper userFileMapper;
    private final UserFolderMapper userFolderMapper;

    @PostMapping("/folders")
    public FolderDto createFolder(@RequestBody CreateFolderDto dto) {
        var folder = userFolderService.createFolder(dto);
        return userFolderMapper.toDto(folder);
    }

    @DeleteMapping("/folders/{id}")
    public void deleteFolder(@PathVariable UUID id) {
        userFolderService.deleteFolder(id);
    }

    @GetMapping("/folders")
    public List<FolderDto> listFolders(@RequestParam(required = false) UUID parentId) {
        return userFolderService.listFolders(parentId);
    }

    @PutMapping("/folders/{id}")
    public void updateFolder(@PathVariable UUID id, @RequestBody UpdateFolderDto dto) {
        userFolderService.updateFolder(id, dto);
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileInfoDto uploadFile(
            @RequestParam("name") String name,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestPart("fileData") MultipartFile fileData
    ) throws IOException {
        byte[] fileBytes = fileData.getBytes();
        UploadFileDto dto = new UploadFileDto(name, folderId, fileBytes);
        return userFileService.uploadFile(dto);
    }

    @GetMapping("/files/{id}")
    public FileInfoDto getFileInfo(@PathVariable UUID id) {
        return userFileService.getFile(id);
    }

    @DeleteMapping("/files/{id}")
    public void deleteFile(@PathVariable UUID id) {
        userFileService.deleteFile(id);
    }

    @GetMapping("/files")
    public List<FileInfoDto> listFiles(@RequestParam UUID folderId) {
        return userFileService.listFiles(folderId);
    }

    @PutMapping("/files/{id}")
    public void updateFile(@PathVariable UUID id, @RequestBody UpdateFileDto dto) {
        userFileService.updateFile(id, dto);
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID id) {
        UserFileEntity file = userFileService.getFileEntity(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.getFileData());
    }

    @GetMapping("/items")
    public FolderContentResponseDto listFolderItems(@RequestParam(required = false) UUID folderId) {
        return userStorageService.getStorageItems(folderId);
    }

    @GetMapping("/bin")
    public List<DeletedItemDto> getBinItems() {
        return userStorageService.getDeletedItems();
    }
}
