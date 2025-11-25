package com.natk.natk_api.clamav.department;

import com.natk.natk_api.clamav.dto.ErrorDto;
import com.natk.natk_api.clamav.dto.VirusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/scan/department")
@RequiredArgsConstructor
@Slf4j
public class FileScanDepartmentController {

    private final DepartmentFileScanService scanService;

    @PostMapping("/{id}/clean")
    public ResponseEntity<Void> clean(@PathVariable UUID id) {
        scanService.markClean(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/infected")
    public ResponseEntity<Void> infected(@PathVariable UUID id, @RequestBody VirusDto dto) {
        scanService.markInfected(id, dto.virus());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/error")
    public ResponseEntity<Void> error(@PathVariable UUID id, @RequestBody ErrorDto dto) {
        scanService.markError(id, dto.errorMessage());
        return ResponseEntity.noContent().build();
    }
}
