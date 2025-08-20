package com.natk.natk_api.llms.service;

import lombok.Getter;

@Getter
public enum ProviderType {
    GEMINI("gemini"),
    GIGA("giga");

    private final String value;

    ProviderType(String value) {
        this.value = value;
    }

}
