package com.natk.natk_api.pdf;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pdf-service")
@Getter
public class PdfServiceProperties {
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }
}
