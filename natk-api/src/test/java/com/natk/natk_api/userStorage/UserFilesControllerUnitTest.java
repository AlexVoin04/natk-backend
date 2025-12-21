package com.natk.natk_api.userStorage;

import com.natk.natk_api.baseStorage.dto.UploadFileDto;
import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.userStorage.service.UserBaseFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFilesControllerUnitTest {

    @Mock
    private UserBaseFileService userFileService;

    @InjectMocks
    private UserStorageController controller; // <-- имя вашего контроллера

    @Test
    void uploadFile_buildsDtoAndDelegates() throws Exception {
        UUID folderId = UUID.randomUUID();
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "fileData", "hello.txt", "text/plain", bytes
        );

        FileInfoDto expected = Mockito.mock(FileInfoDto.class);
        ArgumentCaptor<UploadFileDto> captor = ArgumentCaptor.forClass(UploadFileDto.class);

        when(userFileService.uploadFile(any())).thenReturn(expected);

        FileInfoDto actual = controller.uploadFile("myname.txt", folderId, file);

        assertSame(expected, actual);

        verify(userFileService).uploadFile(captor.capture());
        UploadFileDto dto = captor.getValue();

        assertEquals("myname.txt", dto.name());
        assertEquals(folderId, dto.folderId());
        assertEquals(bytes.length, dto.size());

        // Stream еще никто не читал (сервис замокан), поэтому можно проверить содержимое
        try (InputStream is = dto.fileData()) {  // закрываем ресурс
            assertArrayEquals(bytes, is.readAllBytes());
        }
    }
}