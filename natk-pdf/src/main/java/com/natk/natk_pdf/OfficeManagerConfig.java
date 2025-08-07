package com.natk.natk_pdf;

import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OfficeManagerConfig {

    @Bean(destroyMethod = "stop")
    public LocalOfficeManager officeManager() {
        LocalOfficeManager manager = LocalOfficeManager.builder().install().build();
        try {
            manager.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start LibreOffice", e);
        }
        return manager;
    }
}