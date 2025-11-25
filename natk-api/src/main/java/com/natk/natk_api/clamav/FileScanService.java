package com.natk.natk_api.clamav;

import com.natk.natk_api.baseStorage.FileStatus;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileScanService {

    private final UserFileRepository fileRepo;
    private final MinioFileService minio;

    @Transactional
    public void markClean(UUID id) {
        try {
            UserFileEntity file = getFileOrThrow(id);

            String newKey = minio.generateUserFileKey(file.getCreatedBy().getId());
            minio.copyObjectServerSide("incoming", file.getStorageKey(), "user-files", newKey);
            minio.deleteFile("incoming", file.getStorageKey());

            file.setStorageKey(newKey);
            file.setStatus(FileStatus.READY);

            fileRepo.save(file);
        } catch (Exception e) {
            log.error("markClean failed for {}", id, e);
        }
    }

    @Transactional
    public void markInfected(UUID id, String virusName) {
        try {
            UserFileEntity file = getFileOrThrow(id);
            file.setStatus(FileStatus.INFECTED);
            minio.deleteFile("incoming", file.getStorageKey());
            fileRepo.save(file);
            log.info("VIRUS: {}", virusName);
        } catch (Exception e) {
            log.error("markInfected failed for {}", id, e);
        }
    }

    @Transactional
    public void markError(UUID id, String errorMessage) {
        try {
            UserFileEntity file = fileRepo.findById(id).orElse(null);
            if (file == null) return;

            file.setStatus(FileStatus.ERROR);
            fileRepo.save(file);
            log.info("Error antntivirus: {}", errorMessage);
        } catch (Exception e) {
            log.error("markError failed for {}", id, e);
        }
    }

    private UserFileEntity getFileOrThrow(UUID id) {
        return fileRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + id));
    }
}
