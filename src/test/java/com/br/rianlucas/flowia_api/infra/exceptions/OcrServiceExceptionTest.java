package com.br.rianlucas.flowia_api.infra.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OcrServiceExceptionTest {

    @Test
    void deveCriarComMensagem() {
        OcrServiceException ex = new OcrServiceException("OCR service unavailable");
        assertEquals("OCR service unavailable", ex.getMessage());
    }

    @Test
    void deveCriarComMensagemECausa() {
        Throwable cause = new RuntimeException("Connection refused");
        OcrServiceException ex = new OcrServiceException("OCR error", cause);
        assertEquals("OCR error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
