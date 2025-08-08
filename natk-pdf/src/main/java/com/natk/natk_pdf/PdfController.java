package com.natk.natk_pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class PdfController {

    private final PdfConverter pdfConverter;

    @PostMapping("/convert")
    public ResponseEntity<byte[]> convertToPdf(@RequestParam("file") MultipartFile file) throws IOException {
        String outputName = pdfConverter.buildPdfFileName(file.getOriginalFilename());
        byte[] pdfBytes = pdfConverter.convertToPdf(file.getBytes(), file.getOriginalFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + outputName + "\"")
                .body(pdfBytes);
    }
}
