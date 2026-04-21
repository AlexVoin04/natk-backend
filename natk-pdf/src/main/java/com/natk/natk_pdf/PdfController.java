package com.natk.natk_pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class PdfController {

    private final PdfConverter pdfConverter;

    @PostMapping("/convert")
    public ResponseEntity<byte[]> convertToPdf(@RequestParam("file") MultipartFile file) throws IOException {
        String outputName = pdfConverter.buildPdfFileName(file.getOriginalFilename());
        byte[] pdfBytes = pdfConverter.convertToPdf(file.getBytes(), file.getOriginalFilename());

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(outputName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(pdfBytes);
    }
}
