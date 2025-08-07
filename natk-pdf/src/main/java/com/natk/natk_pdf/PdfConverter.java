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

    private final LocalConverter converter;

    public PdfConverter(LocalOfficeManager officeManager) {
        this.converter = LocalConverter.make(officeManager);
    }

    public byte[] convertToPdf(byte[] inputBytes, String filename){
        File inputFile = null;
        File outputFile = null;

        try {
            inputFile = File.createTempFile("input-", "-" + filename);
            outputFile = File.createTempFile("output-", ".pdf");

            FileUtils.writeByteArrayToFile(inputFile, inputBytes);
            converter.convert(inputFile).to(outputFile).execute();

            return FileUtils.readFileToByteArray(outputFile);
        } catch (OfficeException | IOException e) {
            throw new RuntimeException("PDF conversion failed");
        } finally {
            if (inputFile != null && !inputFile.delete()) {
                System.err.println("Failed to delete temp input file: " + inputFile.getAbsolutePath());
            }
            if (outputFile != null && !outputFile.delete()) {
                System.err.println("Failed to delete temp output file: " + outputFile.getAbsolutePath());
            }
        }
    }
}