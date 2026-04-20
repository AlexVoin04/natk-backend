package com.natk.natk_api.departmentStorage;

import com.natk.natk_api.baseStorage.dto.BulkDeleteResult;
import com.natk.natk_api.baseStorage.dto.CreateFolderDto;
import com.natk.natk_api.baseStorage.dto.FileDownloadDto;
import com.natk.natk_api.baseStorage.dto.FolderTreeDto;
import com.natk.natk_api.baseStorage.dto.MoveFileDto;
import com.natk.natk_api.baseStorage.dto.MoveFolderDto;
import com.natk.natk_api.baseStorage.dto.RenameFileDto;
import com.natk.natk_api.baseStorage.dto.RenameFolderDto;
import com.natk.natk_api.baseStorage.dto.SignedUrlResponse;
import com.natk.natk_api.baseStorage.dto.UploadFileDto;
import com.natk.natk_api.baseStorage.enums.StorageSearchScope;
import com.natk.natk_api.baseStorage.intarfece.DeletionContext;
import com.natk.natk_api.baseStorage.service.BulkDeleteService;
import com.natk.natk_api.departmentStorage.context.DepartmentDeletionContextFactory;
import com.natk.natk_api.departmentStorage.dto.DepartmentDeletedItemDto;
import com.natk.natk_api.departmentStorage.dto.DepartmentFileInfoDto;
import com.natk.natk_api.departmentStorage.dto.DepartmentFolderDto;
import com.natk.natk_api.departmentStorage.dto.DepartmentStorageItemDto;
import com.natk.natk_api.departmentStorage.dto.PurgeItemDto;
import com.natk.natk_api.departmentStorage.service.DepartmentBaseFileService;
import com.natk.natk_api.departmentStorage.service.DepartmentBaseFolderService;
import com.natk.natk_api.departmentStorage.service.DepartmentBaseStorageService;
import com.natk.natk_api.departmentStorage.service.DepartmentPurgeService;
import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/department-storage/{departmentId}")
@RequiredArgsConstructor
public class DepartmentStorageController {
    private final DepartmentBaseFolderService folderService;
    private final DepartmentBaseFileService departmentFileService;
    private final DepartmentBaseStorageService departmentStorageService;
    private final DepartmentPurgeService departmentPurgeService;
    private final BulkDeleteService bulkDeleteService;
    private final DepartmentDeletionContextFactory deptCtxFactory;

    @PostMapping("/folders")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public DepartmentFolderDto createFolder(@PathVariable UUID departmentId, @RequestBody CreateFolderDto dto) {
        return folderService.createFolder(departmentId, dto);
    }

    @PutMapping("/folders/{id}/rename")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public DepartmentFolderDto renameFolder(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @Valid @RequestBody RenameFolderDto dto
    ) {
        return folderService.renameFolder(departmentId, id, dto);
    }

    @PutMapping("/folders/{id}/move")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public DepartmentFolderDto moveFolder(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @Valid @RequestBody MoveFolderDto dto
    ) {
        return folderService.moveFolder(departmentId, id, dto);
    }

    @DeleteMapping("/folders/{id}")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public ResponseEntity<?> deleteFolder(@PathVariable UUID departmentId, @PathVariable UUID id) {
        folderService.deleteFolder(departmentId, id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/folders/{id}/restore")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public DepartmentFolderDto restoreFolder(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID targetParentFolderId
    ) {
        return folderService.restoreFolder(departmentId, id, targetParentFolderId);
    }

    @GetMapping("/folders")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public List<DepartmentFolderDto> listFolders(@PathVariable UUID departmentId, @RequestParam(required = false) UUID parentId) {
        return folderService.listFolders(departmentId, parentId);
    }

    @GetMapping("/folders/tree")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public List<FolderTreeDto> getFolderTree(@PathVariable UUID departmentId) {
        return folderService.getFolderTree(departmentId);
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public DepartmentFileInfoDto uploadFile(
            @PathVariable UUID departmentId,
            @RequestParam("name") String name,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestPart("fileData") MultipartFile fileData
    ) throws IOException {
        UploadFileDto dto = new UploadFileDto(name, folderId, fileData.getInputStream(), fileData.getSize());
        return departmentFileService.uploadFile(dto, departmentId);
    }

    @GetMapping("/files/{id}")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public DepartmentFileInfoDto getFileInfo(
            @PathVariable UUID departmentId,
            @PathVariable UUID id
    ) {
        return departmentFileService.getFile(id, departmentId);
    }

    @DeleteMapping("/files/{id}")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public ResponseEntity<?> deleteFile(
            @PathVariable UUID departmentId,
            @PathVariable UUID id
    ) {
        departmentFileService.deleteFile(id, departmentId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/files/{id}/restore")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public DepartmentFileInfoDto restoreFile(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID targetFolderId
    ) {
        return departmentFileService.restoreFile(id, targetFolderId, departmentId);
    }

    @GetMapping("/files")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public List<DepartmentFileInfoDto> listFiles(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) UUID folderId
    ) {
        return departmentFileService.listFiles(folderId, departmentId);
    }

    @PutMapping("/files/{id}/rename")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public DepartmentFileInfoDto renameFile(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @Valid @RequestBody RenameFileDto dto
    ) {
        return departmentFileService.renameFile(id, dto.newName(), departmentId);
    }

    @PutMapping("/files/{id}/move")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public DepartmentFileInfoDto moveFile(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @Valid @RequestBody MoveFileDto dto
    ) {
        return departmentFileService.moveFile(id, dto.newFolderId(), dto.moveToRoot(), departmentId);
    }

    @GetMapping("/files/{id}/download")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @PathVariable UUID departmentId,
            @PathVariable UUID id
    ) {
        FileDownloadDto dto = departmentFileService.getFileDownloadData(id, departmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dto.translitName() + "\"; filename*=UTF-8''" + dto.encodedName())
                .contentType(dto.fileType())
                .body(dto.body());
    }

    @PostMapping("/files/{id}/copy")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public DepartmentFileInfoDto copyFile(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID targetFolderId
    ) {
        return departmentFileService.copyFile(id, targetFolderId, departmentId);
    }

    @GetMapping("/files/{id}/signed-url")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public SignedUrlResponse getDepartmentFileSignedUrl(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @RequestParam(name = "expires", required = false, defaultValue = "300") int expiresSeconds
    ) {
        return departmentFileService.getFileSignedUrl(id, departmentId, expiresSeconds);
    }

    @GetMapping("/items")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'ACCESS')")
    public FolderContentResponseDto<DepartmentStorageItemDto> listFolderItems(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) UUID folderId
    ) {
        return departmentStorageService.getStorageItems(folderId, departmentId);
    }

    @DeleteMapping("/items")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public ResponseEntity<BulkDeleteResult> deleteItems(
            @PathVariable UUID departmentId,
            @RequestBody List<PurgeItemDto> items
    ) {
        DeletionContext ctx = deptCtxFactory.forDepartment(departmentId);
        BulkDeleteResult result = bulkDeleteService.deleteMultiple(items, ctx);
        return ResponseEntity.ok(result);
    }


    //TODO: folderId - задаток на поиск внутри папки
    @GetMapping("/search")
    public List<DepartmentStorageItemDto> search(
            @PathVariable UUID departmentId,
            @RequestParam String q,
            @RequestParam(defaultValue = "BOTH") StorageSearchScope scope,
            @RequestParam(required = false) UUID folderId
    ) {
        return departmentStorageService.searchItems(q, scope, folderId, departmentId);
    }

    @GetMapping("/bin")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public List<DepartmentDeletedItemDto> getBinItems(
            @PathVariable UUID departmentId
    ) {
        return departmentStorageService.getDeletedItems(departmentId);
    }

    @DeleteMapping("/bin/files/{id}/purge")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public ResponseEntity<?> purgeDeletedFile(
            @PathVariable UUID departmentId,
            @PathVariable UUID id
    ) {
        departmentPurgeService.purgeFile(id, departmentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bin/folders/{id}/purge")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public ResponseEntity<?> purgeDeletedFolder(
            @PathVariable UUID departmentId,
            @PathVariable UUID id
    ) {
        departmentPurgeService.purgeFolder(id, departmentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bin/purge")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public ResponseEntity<?> purgeMultiple(
            @PathVariable UUID departmentId,
            @RequestBody List<PurgeItemDto> items
    ) {
        departmentPurgeService.purgeMultiple(items, departmentId);
        return ResponseEntity.ok().build();
    }
}