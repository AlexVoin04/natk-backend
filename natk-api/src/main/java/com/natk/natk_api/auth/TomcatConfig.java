package com.natk.natk_api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer(
            @Value("${spring.servlet.multipart.max-file-size}") String maxFileSize
    ) {
        return factory -> factory.addConnectorCustomizers(connector -> {
            long maxSize = DataSize.parse(maxFileSize).toBytes();
            connector.setMaxPostSize((int) Math.min(maxSize, Integer.MAX_VALUE)); // Tomcat max int
            connector.setMaxSavePostSize((int) Math.min(maxSize, Integer.MAX_VALUE));
        });
    }
}
