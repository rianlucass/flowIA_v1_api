package com.br.rianlucas.flowia_api.infra.exceptions;

public class OcrServiceException extends RuntimeException {
    public OcrServiceException(String message) {
        super(message);
    }

    public OcrServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
