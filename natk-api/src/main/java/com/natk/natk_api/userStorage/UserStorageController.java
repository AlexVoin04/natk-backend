package com.natk.natk_api.userStorage;

import com.natk.natk_api.baseStorage.PurgeStats;
import com.natk.natk_api.baseStorage.dto.MoveFileDto;
import com.natk.natk_api.baseStorage.dto.MoveFolderDto;
import com.natk.natk_api.baseStorage.dto.RenameFileDto;
import com.natk.natk_api.baseStorage.dto.RenameFolderDto;
import com.natk.natk_api.departmentStorage.dto.PurgeItemDto;
import com.natk.natk_api.llms.dto.QuestionRequestDto;
import com.natk.natk_api.llms.dto.QuestionResponseDto;
import com.natk.natk_api.llms.service.QuestionGenerationService;
import com.natk.natk_api.baseStorage.dto.CreateFolderDto;
import com.natk.natk_api.baseStorage.dto.FileDownloadDto;
import com.natk.natk_api.baseStorage.dto.FolderTreeDto;
import com.natk.natk_api.baseStorage.dto.UploadFileDto;
import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.dto.UserDeletedItemDto;
import com.natk.natk_api.userStorage.dto.UserStorageItemDto;
import com.natk.natk_api.userStorage.service.UserBaseFileService;
import com.natk.natk_api.userStorage.service.UserBaseFolderService;
import com.natk.natk_api.userStorage.service.UserBaseStorageService;
import com.natk.natk_api.userStorage.service.UserPurgeService;
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class UserStorageController {
    private final UserBaseFileService userFileService;
    private final UserBaseFolderService userFolderService;
    private final UserBaseStorageService userStorageService;
    private final QuestionGenerationService questionGenerationService;
    private final UserPurgeService userPurgeService;

    @PostMapping("/folders")
    public FolderDto createFolder(@RequestBody CreateFolderDto dto) {
        return userFolderService.createFolder(dto);
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<?> deleteFolder(@PathVariable UUID id) {
        userFolderService.deleteFolder(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/folders/{id}/restore")
    public FolderDto restoreFolder(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID targetParentFolderId
    ) {
        return userFolderService.restoreFolder(id, targetParentFolderId);
    }

    @GetMapping("/folders")
    public List<FolderDto> listFolders(@RequestParam(required = false) UUID parentId) {
        return userFolderService.listFolders(parentId);
    }

    @PutMapping("/folders/{id}/rename")
    public FolderDto renameFolder(@PathVariable UUID id, @Valid @RequestBody RenameFolderDto dto) {
        return userFolderService.renameFolder(id, dto);
    }

    @PutMapping("/folders/{id}/move")
    public FolderDto moveFolder(
            @PathVariable UUID id,
            @Valid @RequestBody MoveFolderDto dto
    ) {
        return userFolderService.moveFolder(id, dto);
    }

    @GetMapping("/folders/tree")
    public List<FolderTreeDto> getFolderTree() {
        return userFolderService.getFolderTree();
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileInfoDto uploadFile(
            @RequestParam("name") String name,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestPart("fileData") MultipartFile fileData
    ) throws IOException {
        UploadFileDto dto = new UploadFileDto(name, folderId, fileData.getInputStream(), fileData.getSize());
        return userFileService.uploadFile(dto);
    }

    @GetMapping("/files/{id}")
    public FileInfoDto getFileInfo(@PathVariable UUID id) {
        return userFileService.getFile(id);
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable UUID id) {
        userFileService.deleteFile(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/files/{id}/restore")
    public FileInfoDto restoreFile(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID targetFolderId
    ) {
        return userFileService.restoreFile(id, targetFolderId);
    }

    @GetMapping("/files")
    public List<FileInfoDto> listFiles(@RequestParam UUID folderId) {
        return userFileService.listFiles(folderId);
    }

    @PutMapping("/files/{id}/rename")
    public FileInfoDto renameFile(@PathVariable UUID id, @Valid @RequestBody RenameFileDto newName) {
        return userFileService.renameFile(id, newName.newName());
    }

    @PutMapping("/files/{id}/move")
    public FileInfoDto moveFile(@PathVariable UUID id, @RequestBody MoveFileDto dto) {
        return userFileService.moveFile(id, dto.newFolderId(), dto.moveToRoot());
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable UUID id) {
        FileDownloadDto dto = userFileService.getFileDownloadData(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dto.translitName() + "\"; filename*=UTF-8''" + dto.encodedName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(dto.body());
    }

    @PostMapping("files/generate-questions")
    public ResponseEntity<QuestionResponseDto> generateQuestions(@RequestBody @Valid QuestionRequestDto dto) {
        String result = questionGenerationService.generateQuestions(dto.fileIds(), dto.questionCounts(), dto.provider());
        return ResponseEntity.ok(new QuestionResponseDto(result));
    }

    @PostMapping("/files/{id}/copy")
    public FileInfoDto copyFile(@PathVariable UUID id, @RequestParam(required = false) UUID targetFolderId) {
        return userFileService.copyFile(id, targetFolderId);
    }

    @GetMapping("/items")
    public FolderContentResponseDto<UserStorageItemDto> listFolderItems(@RequestParam(required = false) UUID folderId) {
        return userStorageService.getStorageItems(folderId);
    }

    @GetMapping("/bin")
    public List<UserDeletedItemDto> getBinItems() {
        return userStorageService.getDeletedItems();
    }

    @DeleteMapping("/bin/files/{id}/purge")
    public ResponseEntity<?> purgeDeletedFile(@PathVariable UUID id) {
        userPurgeService.purgeFile(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bin/folders/{id}/purge")
    public ResponseEntity<?> purgeDeletedFolder(@PathVariable UUID id) {
        userPurgeService.purgeFolder(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bin/purge")
    public PurgeStats purgeMultiple(@RequestBody List<PurgeItemDto> items) {
        return userPurgeService.purgeMultiple(items);
    }

    //TODO: добавить просто очистку без items
}
