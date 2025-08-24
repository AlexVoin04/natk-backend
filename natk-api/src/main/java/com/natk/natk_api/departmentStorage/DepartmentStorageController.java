package com.natk.natk_api.departmentStorage;

import com.natk.natk_api.departmentStorage.dto.DepartmentFolderDto;
import com.natk.natk_api.departmentStorage.service.DepartmentBaseFolderService;
import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderTreeDto;
import com.natk.natk_api.userStorage.dto.UpdateFolderDto;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/department-storage/{departmentId}")
@RequiredArgsConstructor
public class DepartmentStorageController {
    private final DepartmentBaseFolderService folderService;

    @PostMapping("/folders")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, #departmentId)")
    public DepartmentFolderDto createFolder(@PathVariable UUID departmentId, @RequestBody CreateFolderDto dto) {
        return folderService.createFolder(dto);
    }

    @PutMapping("/folders/{id}")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, #departmentId)")
    public DepartmentFolderDto updateFolder(@PathVariable UUID departmentId, @PathVariable UUID id, @RequestBody UpdateFolderDto dto) {
        return folderService.updateFolder(id, dto);
    }

    @DeleteMapping("/folders/{id}")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, #departmentId)")
    public ResponseEntity<?> deleteFolder(@PathVariable UUID departmentId, @PathVariable UUID id) {
        folderService.deleteFolder(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/folders/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, #departmentId)")
    public DepartmentFolderDto restoreFolder(
            @PathVariable UUID departmentId,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID targetParentFolderId
    ) {
        return folderService.restoreFolder(id, targetParentFolderId);
    }

    @GetMapping("/folders")
    public List<DepartmentFolderDto> listFolders(@RequestParam(required = false) UUID parentId) {
        return folderService.listFolders(parentId);
    }

    @GetMapping("/folders/tree")
    public List<FolderTreeDto> getFolderTree() {
        return folderService.getFolderTree();
    }
}
