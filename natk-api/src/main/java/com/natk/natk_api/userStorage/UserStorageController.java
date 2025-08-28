package com.natk.natk_api.userStorage;

import com.natk.natk_api.baseStorage.dto.MoveFolderDto;
import com.natk.natk_api.baseStorage.dto.RenameFolderDto;
import com.natk.natk_api.llms.dto.QuestionRequestDto;
import com.natk.natk_api.llms.dto.QuestionResponseDto;
import com.natk.natk_api.llms.service.QuestionGenerationService;
import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.DeletedItemDto;
import com.natk.natk_api.userStorage.dto.FileDownloadDto;
import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.dto.FolderTreeDto;
import com.natk.natk_api.userStorage.dto.UpdateFileDto;
import com.natk.natk_api.userStorage.dto.UploadFileDto;
import com.natk.natk_api.userStorage.service.UserBaseFolderService;
import com.natk.natk_api.userStorage.service.UserFileService;
import com.natk.natk_api.userStorage.service.UserStorageService;
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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class UserStorageController {
    private final UserFileService userFileService;
    private final UserBaseFolderService userFolderService;
    private final UserStorageService userStorageService;
    private final QuestionGenerationService questionGenerationService;

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
    public FolderDto renameFolder(@PathVariable UUID id, @RequestBody RenameFolderDto dto) {
        return userFolderService.renameFolder(id, dto);
    }

    @PutMapping("/folders/{id}/move")
    public FolderDto moveFolder(
            @PathVariable UUID id,
            @RequestBody MoveFolderDto dto
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
        byte[] fileBytes = fileData.getBytes();
        UploadFileDto dto = new UploadFileDto(name, folderId, fileBytes);
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

    @PutMapping("/files/{id}")
    public FileInfoDto updateFile(@PathVariable UUID id, @RequestBody UpdateFileDto dto) {
        return userFileService.updateFile(id, dto);
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID id) {
        FileDownloadDto dto = userFileService.getFileDownloadData(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dto.translitName() + "\"; filename*=UTF-8''" + dto.encodedName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(dto.fileData());
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
    public FolderContentResponseDto listFolderItems(@RequestParam(required = false) UUID folderId) {
        return userStorageService.getStorageItems(folderId);
    }

    @GetMapping("/bin")
    public List<DeletedItemDto> getBinItems() {
        return userStorageService.getDeletedItems();
    }
}
