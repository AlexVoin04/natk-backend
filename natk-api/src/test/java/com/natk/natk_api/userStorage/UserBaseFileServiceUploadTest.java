package com.natk.natk_api.userStorage;

import com.natk.common.messaging.ScanTask;
import com.natk.natk_api.baseStorage.MagicValidationResult;
import com.natk.natk_api.baseStorage.dto.UploadFileDto;
import com.natk.natk_api.baseStorage.service.TransliterationService;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.rabbit.ScanTaskPublisher;
import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.userStorage.mapper.UserFileMapper;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.userStorage.service.UserBaseFileService;
import com.natk.natk_api.userStorage.service.UserFileNameResolverService;
import com.natk.natk_api.userStorage.service.UserUploadStrategy;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBaseFileServiceUploadTest {

    @Mock private UserFileRepository fileRepo;
    @Mock private UserFolderRepository folderRepo;
    @Mock private CurrentUserService currentUserService;
    @Mock private UserFileMapper fileMapper;
    @Mock private UserFileNameResolverService fileNameResolverService;
    @Mock private TransliterationService transliterationService;
    @Mock private MinioFileService minioFileService;
    @Mock private ScanTaskPublisher scanTaskPublisher;
    @Mock private UserUploadStrategy userUploadStrategy;

    private UserBaseFileService service;

    @BeforeEach
    void setUp() {
        service = new UserBaseFileService(
                fileRepo,
                folderRepo,
                currentUserService,
                fileMapper,
                fileNameResolverService,
                transliterationService,
                minioFileService,
                scanTaskPublisher,
                userUploadStrategy
        );
    }

    @Test
    void uploadFile_uploadsFullStream_andPublishesAfterCommit() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        when(currentUserService.getCurrentUser()).thenReturn(user);

        byte[] fullBytes = "0123456789".getBytes(StandardCharsets.UTF_8);
        InputStream data = new ByteArrayInputStream(fullBytes);

        UploadFileDto dto = new UploadFileDto("a.txt", null, data, fullBytes.length);

        when(userUploadStrategy.getOwner(any())).thenReturn(user);
        doNothing().when(userUploadStrategy).ensureUniqueNameOrThrow(eq("a.txt"), isNull(), eq(user));

        // validateMime "съедает" первые 4 байта из InputStream и возвращает их как header
        when(userUploadStrategy.validateMime(any(InputStream.class), eq("a.txt")))
                .thenAnswer(inv -> {
                    InputStream is = inv.getArgument(0);
                    byte[] header = is.readNBytes(4);
                    return new MagicValidationResult(header, "text/plain");
                });

        when(userUploadStrategy.generateIncomingKey(eq(user))).thenReturn("incoming-key");

        UserFileEntity newEntity = new UserFileEntity();
        newEntity.setId(UUID.randomUUID());

        when(userUploadStrategy.buildNewFileEntity(
                eq("a.txt"),
                eq("text/plain"),
                eq((long) fullBytes.length),
                isNull(),
                eq("incoming-key"),
                any(),
                eq(user)
        )).thenReturn(newEntity);

        // uploadToMinio: проверяем, что stream содержит ВЕСЬ файл
        doAnswer(inv -> {
            InputStream stream = inv.getArgument(0);
            long size = inv.getArgument(1);
            String key = inv.getArgument(2);
            String mime = inv.getArgument(3);

            assertEquals(fullBytes.length, size);
            assertEquals("incoming-key", key);
            assertEquals("text/plain", mime);

            byte[] uploaded = stream.readAllBytes();
            assertArrayEquals(fullBytes, uploaded);
            return null;
        }).when(userUploadStrategy).uploadToMinio(any(InputStream.class), anyLong(), anyString(), anyString());

        doNothing().when(userUploadStrategy).persistFile(eq(newEntity));
        when(userUploadStrategy.reloadAfterSave(eq(newEntity))).thenReturn(newEntity);

        ScanTask task = Mockito.mock(ScanTask.class);
        when(userUploadStrategy.buildScanTask(eq(newEntity), eq("incoming-key"), any(), eq(user))).thenReturn(task);

        FileInfoDto out = Mockito.mock(FileInfoDto.class);
        when(fileMapper.toDto(eq(newEntity))).thenReturn(out);

        //when: транзакция + commit
        FileInfoDto result = runInTxAndReturn(() -> {
            FileInfoDto inside = service.uploadFile(dto);
            verify(scanTaskPublisher, never()).publish(any());
            verifyNoInteractions(folderRepo);
            return inside;
        });

        assertSame(out, result);
        verify(scanTaskPublisher, times(1)).publish(eq(task));

        // Полный порядок вызовов strategy
        InOrder inOrder = inOrder(userUploadStrategy);
        inOrder.verify(userUploadStrategy).getOwner(any());
        inOrder.verify(userUploadStrategy).ensureUniqueNameOrThrow(eq("a.txt"), isNull(), eq(user));
        inOrder.verify(userUploadStrategy).validateMime(same(data), eq("a.txt"));
        inOrder.verify(userUploadStrategy).generateIncomingKey(eq(user));
        inOrder.verify(userUploadStrategy).buildNewFileEntity(
                eq("a.txt"),
                eq("text/plain"),
                eq((long) fullBytes.length),
                isNull(),
                eq("incoming-key"),
                any(),
                eq(user)
        );
        inOrder.verify(userUploadStrategy).uploadToMinio(
                any(InputStream.class),
                eq((long) fullBytes.length),
                eq("incoming-key"),
                eq("text/plain"));
        inOrder.verify(userUploadStrategy).persistFile(eq(newEntity));
        inOrder.verify(userUploadStrategy).reloadAfterSave(eq(newEntity));
        inOrder.verify(userUploadStrategy).buildScanTask(eq(newEntity), eq("incoming-key"), any(), eq(user));
        inOrder.verifyNoMoreInteractions(); // проверка, что после последней проверенной операции на mock больше ничего не происходило
    }

    private FileInfoDto runInTxAndReturn(java.util.function.Supplier<FileInfoDto> action) {
        TransactionTemplate tt = new TransactionTemplate(new ResourcelessTransactionManager());
        return tt.execute(status -> action.get());
    }
}

