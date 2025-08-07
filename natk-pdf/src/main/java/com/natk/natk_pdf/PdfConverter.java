package com.natk.natk_pdf;

import org.apache.commons.io.FileUtils;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class PdfConverter {

    private final LocalOfficeManager officeManager;
    private final LocalConverter converter;

    public PdfConverter() {
        this.officeManager = LocalOfficeManager.builder().install().build();
        try {
            officeManager.start();
        } catch (OfficeException e) {
            throw new RuntimeException("Failed to start LibreOffice", e);
        }

        this.converter = LocalConverter.make(officeManager);
    }

    public byte[] convertToPdf(byte[] inputBytes, String filename) throws IOException, OfficeException {
        File inputFile = File.createTempFile("input-", "-" + filename);
        File outputFile = File.createTempFile("output-", ".pdf");

        FileUtils.writeByteArrayToFile(inputFile, inputBytes);
        converter.convert(inputFile).to(outputFile).execute();

        return FileUtils.readFileToByteArray(outputFile);
    }
}