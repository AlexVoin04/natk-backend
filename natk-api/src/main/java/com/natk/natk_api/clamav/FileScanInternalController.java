package com.natk.natk_api.clamav;

import com.natk.natk_api.baseStorage.FileStatus;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/internal/scan")
@RequiredArgsConstructor
public class FileScanInternalController {

    private final UserFileRepository fileRepo;
    private final MinioFileService minioFileService;

    @PostMapping("/{id}/clean")
    public void markClean(@PathVariable UUID id, @RequestBody CleanDto dto) throws Exception {
        UserFileEntity file = fileRepo.findById(id).orElseThrow();
        String newKey = minioFileService.generateUserFileKey(file.getCreatedBy().getId());
        minioFileService.copyObjectServerSide("incoming", file.getStorageKey(), newKey);
        minioFileService.deleteFile("incoming", file.getStorageKey());

        file.setStorageKey(newKey);
        file.setStatus(FileStatus.READY);

        fileRepo.save(file);
    }

    @PostMapping("/{id}/infected")
    public void markInfected(@PathVariable UUID id, @RequestBody VirusDto dto) {
        UserFileEntity f = fileRepo.findById(id).orElseThrow();
        f.setStatus(FileStatus.INFECTED);
        minioFileService.deleteFile("incoming", f.getStorageKey());
//        f.setVirusInfo(dto.virus());
        fileRepo.save(f);
    }

    @PostMapping("/{id}/error")
    public void markError(@PathVariable UUID id, @RequestBody ErrorDto dto) {
        Optional<UserFileEntity> scanOpt = fileRepo.findById(id);
        if (scanOpt.isPresent()) {
            UserFileEntity scan = scanOpt.get();
            scan.setStatus(FileStatus.ERROR);
            fileRepo.save(scan);
            // обработка ошибки
        } else {
//            log.warn("Scan record not found for id {}", scanId);
            // можно вернуть 404 или просто игнорировать
        }

//        UserFileEntity f = fileRepo.findById(id).orElseThrow();
//        f.setStatus(FileStatus.ERROR);
//        f.setVirusInfo(dto.errorMessage());
//        fileRepo.save(f);
    }

    public record CleanDto(String storageKey) {}
    public record VirusDto(String virus) {}
    public record ErrorDto(String errorMessage) {}
}
