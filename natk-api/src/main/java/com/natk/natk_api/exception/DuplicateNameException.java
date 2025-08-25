package com.natk.natk_api.exception;

public class DuplicateNameException extends RuntimeException {
    private final String suggestedName;

    public DuplicateNameException(String message, String suggestedName) {
        super(message);
        this.suggestedName = suggestedName;
    }

    public String getSuggestedName() {
        return suggestedName;
    }
}
