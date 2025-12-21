package com.natk.natk_api.minIO;

import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.minio.MinioMetrics;
import com.natk.natk_api.minio.MinioRetryableService;
import com.natk.natk_api.minio.SecureKeyGenerator;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioFileServiceTest {

    @Mock private MinioRetryableService retryable;
    @Mock private MinioClient minioClient;
    @Mock private MinioMetrics metrics;
    @Mock private SecureKeyGenerator keyGenerator;

    private MinioFileService service;

    @BeforeEach
    void setUp() {
        service = new MinioFileService(retryable, minioClient, metrics, keyGenerator);
    }

    @Test
    void uploadFile_createsBucketWhenMissing_putsObject_recordsMetrics() throws Exception {
        String bucket = "incoming";
        String objectKey = "k1";
        String mime = "application/pdf";
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        InputStream is = new ByteArrayInputStream(bytes);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        service.uploadFile(is, bytes.length, bucket, objectKey, mime);

        // bucket created
        ArgumentCaptor<BucketExistsArgs> bucketExistsCaptor = ArgumentCaptor.forClass(BucketExistsArgs.class);
        verify(minioClient).bucketExists(bucketExistsCaptor.capture());
        assertEquals(bucket, bucketExistsCaptor.getValue().bucket()); // bucket() доступен через BucketArgs [page:2]

        ArgumentCaptor<MakeBucketArgs> makeBucketCaptor = ArgumentCaptor.forClass(MakeBucketArgs.class);
        verify(minioClient).makeBucket(makeBucketCaptor.capture());
        assertEquals(bucket, makeBucketCaptor.getValue().bucket()); // bucket() доступен через BucketArgs [page:2]

        // putObject args are correct
        ArgumentCaptor<PutObjectArgs> putCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(retryable).putObject(putCaptor.capture());

        PutObjectArgs put = putCaptor.getValue();
        assertEquals(bucket, put.bucket());                 // PutObjectArgs.bucket() [web:113]
        assertEquals(objectKey, put.object());              // PutObjectArgs.object() [web:113]
        assertEquals(mime, put.contentType());              // PutObjectArgs.contentType() [web:113]
        assertNotNull(put.stream());                        // PutObjectArgs.stream() [web:113]

        // metrics
        verify(metrics).recordUpload(eq((long) bytes.length), anyLong());
        verify(metrics, never()).recordError();

        verifyNoMoreInteractions(metrics);
    }

    @Test
    void uploadFile_bucketCached_bucketExistsCalledOnce() throws Exception {
        String bucket = "incoming";
        String objectKey1 = "k1";
        String objectKey2 = "k2";

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        service.uploadFile(new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)), 1, bucket, objectKey1, "text/plain");
        service.uploadFile(new ByteArrayInputStream("2".getBytes(StandardCharsets.UTF_8)), 1, bucket, objectKey2, "text/plain");

        // Из-за bucketCache второй раз bucketExists/makeBucket дергаться не должны
        verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));

        verify(retryable, times(2)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadFile_whenPutObjectFails_recordsError_andThrowsRuntimeException() throws Exception {
        String bucket = "incoming";
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        doThrow(new RuntimeException("minio down"))
                .when(retryable).putObject(any(PutObjectArgs.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.uploadFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), 3, bucket, "k", "application/octet-stream")
        );

        assertTrue(ex.getMessage().contains("Не удалось загрузить файл в MinIO"));

        verify(metrics).recordError();
        verify(metrics, never()).recordUpload(anyLong(), anyLong());
    }

    @Test
    void downloadFile_returnsStream_recordsDownloadMetrics() throws Exception {
        String bucket = "user-files";
        String key = "obj";
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(retryable.getObject(any(GetObjectArgs.class))).thenReturn(new ByteArrayInputStream(bytes));

        try (InputStream is = service.downloadFile(bucket, key)) { // <- закрываем ресурс
            assertArrayEquals(bytes, is.readAllBytes());
        }

        verify(metrics).recordDownload(anyLong());
        verify(metrics, never()).recordError();
    }

    @Test
    void downloadFileAsBytes_delegatesToDownload_andReadsAllBytes() throws Exception {
        String bucket = "user-files";
        String key = "obj";
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(retryable.getObject(any(GetObjectArgs.class))).thenReturn(new ByteArrayInputStream(bytes));

        byte[] out = service.downloadFileAsBytes(bucket, key);
        assertArrayEquals(bytes, out);

        verify(metrics).recordDownload(anyLong());
        verify(metrics, never()).recordError();
    }

    @Test
    void deleteFile_callsRemoveObject() throws Exception {
        String bucket = "user-files";
        String key = "obj";

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        service.deleteFile(bucket, key);

        ArgumentCaptor<RemoveObjectArgs> cap = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(retryable).removeObject(cap.capture());
        assertEquals(bucket, cap.getValue().bucket()); // bucket() через BucketArgs [page:2]
        assertEquals(key, cap.getValue().object());    // object() через ObjectArgs.Builder-предков (обычно доступен)

        verify(metrics, never()).recordError();
    }

    @Test
    void deleteFiles_continuesOnError_doesNotThrow() throws Exception {
        String bucket = "user-files";
        List<String> keys = List.of("k1", "k2", "k3");

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        // Сделаем так, чтобы removeObject падал только на первом вызове
        AtomicInteger n = new AtomicInteger();
        doAnswer(inv -> {
            if (n.incrementAndGet() == 1) throw new RuntimeException("boom");
            return null;
        }).when(retryable).removeObject(any(RemoveObjectArgs.class));

        assertDoesNotThrow(() -> service.deleteFiles(bucket, keys));

        verify(retryable, times(3)).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void generateKeys_formatsCorrectly() {
        when(keyGenerator.generate256BitKey()).thenReturn("RANDOM");

        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID depId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        assertEquals("user/11111111-1111-1111-1111-111111111111/file/RANDOM", service.generateUserFileKey(userId));
        assertEquals("incoming/user/11111111-1111-1111-1111-111111111111/file/RANDOM", service.generateIncomingUserFileKey(userId));
        assertEquals("department/22222222-2222-2222-2222-222222222222/file/RANDOM", service.generateDepartmentFileKey(depId));
        assertEquals("incoming/department/22222222-2222-2222-2222-222222222222/file/RANDOM", service.generateIncomingDepartmentFileKey(depId));
    }
}
