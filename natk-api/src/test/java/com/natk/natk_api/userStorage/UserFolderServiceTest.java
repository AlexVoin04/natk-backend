package com.natk.natk_api.userStorage;

import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.dto.UpdateFolderDto;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.userStorage.service.UserFolderService;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFolderServiceTest {

    @Mock private UserFolderRepository folderRepo;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private UserFolderService userFolderService;

    private final UUID folderId = UUID.randomUUID();
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(UUID.randomUUID());
    }

    @Test
    void createFolder_success() {
        CreateFolderDto dto = new CreateFolderDto("docs", null);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(folderRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FolderDto created = userFolderService.createFolder(dto);

        assertEquals("docs", created.name());
    }

    @Test
    void deleteFolder_success() {
        UserFolderEntity folder = new UserFolderEntity();
        folder.setId(folderId);
        folder.setUser(user);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));

        userFolderService.deleteFolder(folderId);

        verify(folderRepo).delete(folder);
    }

    @Test
    void listFolders_success() {
        UserFolderEntity folder = new UserFolderEntity();
        folder.setId(folderId);
        folder.setName("docs");
        folder.setUser(user);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(folderRepo.findByUserAndParentFolderAndIsDeletedFalse(eq(user), isNull())).thenReturn(List.of(folder));

        List<FolderDto> result = userFolderService.listFolders(null);

        assertEquals(1, result.size());
        assertEquals("docs", result.get(0).name());
    }

    @Test
    void updateFolder_success() {
        UpdateFolderDto dto = new UpdateFolderDto("new", null);
        UserFolderEntity folder = new UserFolderEntity();
        folder.setId(folderId);
        folder.setUser(user);
        folder.setName("old");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));

        userFolderService.updateFolder(folderId, dto);

        assertEquals("new", folder.getName());
    }
}