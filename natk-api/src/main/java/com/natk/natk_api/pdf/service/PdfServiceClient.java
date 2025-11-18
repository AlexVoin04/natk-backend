package com.natk.natk_api.pdf.service;

import com.natk.natk_api.pdf.PdfServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PdfServiceClient {

    private final RestClient restClient;
    private final PdfServiceProperties pdfServiceProperties;

    public byte[] convertToPdf(String fileName, byte[] fileData) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(fileData) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        MultiValueMap<String, ?> body = builder.build();

        return restClient.post()
                .uri(pdfServiceProperties.getUrl() + "/convert")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(byte[].class);
    }
}
