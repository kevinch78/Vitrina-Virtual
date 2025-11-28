package com.vitrina.vitrinaVirtual.domain.service;

public class OutfitGenerationException extends RuntimeException {

    public OutfitGenerationException(String message) {
        super(message);
    }

    public OutfitGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}