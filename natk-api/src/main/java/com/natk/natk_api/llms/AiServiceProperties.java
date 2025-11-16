package com.natk.natk_api.llms;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai-service")
@Getter
public class AiServiceProperties {
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }
}
