package com.natk.natk_api.llms.service;
import jakarta.annotation.PreDestroy;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

//@Component
//public class PdfConverter {
//
//    private final OfficeManager officeManager;
//    private final DocumentConverter converter;
//
//    public PdfConverter() {
//        officeManager = LocalOfficeManager.builder().install().build();
//        try {
//            officeManager.start();
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to start LibreOffice", e);
//        }
//
//        converter = LocalConverter.builder()
//                .officeManager(officeManager)
//                .build();
//    }
//
//    public byte[] convertToPdf(byte[] inputBytes, String fileName) throws IOException, OfficeException {
//        File inputFile = File.createTempFile("upload-", "-" + fileName);
//        FileUtils.writeByteArrayToFile(inputFile, inputBytes);
//
//        File outputFile = File.createTempFile("converted-", ".pdf");
//
//        converter.convert(inputFile).to(outputFile).execute();
//
//        return FileUtils.readFileToByteArray(outputFile);
//    }
//
//    @PreDestroy
//    public void stop() {
//        if (officeManager != null) {
//            try {
//                officeManager.stop();
//            } catch (Exception ignored) { }
//        }
//    }
//}
