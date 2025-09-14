package com.natk.natk_api.userStorage;


import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.baseStorage.dto.UploadFileDto;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.userStorage.service.UserFileService;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserFileServiceTest {
    @Mock private UserFileRepository fileRepo;
    @Mock private UserFolderRepository folderRepo;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private UserFileService userFileService;

    private final UUID fileId = UUID.randomUUID();
    private final UUID folderId = UUID.randomUUID();
    private UserEntity user;
    private UserFolderEntity folder;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(UUID.randomUUID());

        folder = new UserFolderEntity();
        folder.setId(folderId);
        folder.setUser(user);
    }

    @Test
    void uploadFile_success() {
        UploadFileDto dto = new UploadFileDto("file.txt", folderId, "data".getBytes());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(fileRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FileInfoDto file = userFileService.uploadFile(dto);

        assertEquals("text/plain", file.fileType());
        assertEquals("file.txt", file.name());
    }

    @Test
    void getFile_success() {
        UserFileEntity file = new UserFileEntity();
        file.setId(fileId);
        file.setName("test.txt");
        file.setCreatedBy(user);
        file.setFileType("text/plain");
        file.setCreatedAt(Instant.now());

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(fileRepo.findById(fileId)).thenReturn(Optional.of(file));

        FileInfoDto dto = userFileService.getFile(fileId);

        assertEquals(fileId, dto.id());
    }

    @Test
    void deleteFile_success() {
        UserFileEntity file = new UserFileEntity();
        file.setId(fileId);
        file.setCreatedBy(user);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(fileRepo.findById(fileId)).thenReturn(Optional.of(file));

        userFileService.deleteFile(fileId);

        verify(fileRepo).delete(file);
    }

    @Test
    void listFiles_success() {
        UserFileEntity file = new UserFileEntity();
        file.setId(fileId);
        file.setName("name");
        file.setCreatedBy(user);
        file.setFileType("txt");
        file.setCreatedAt(Instant.now());
        file.setFolder(folder);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(fileRepo.findByFolderAndIsDeletedFalse(folder)).thenReturn(List.of(file));

        List<FileInfoDto> files = userFileService.listFiles(folderId);

        assertEquals(1, files.size());
        assertEquals(fileId, files.get(0).id());
    }
}
