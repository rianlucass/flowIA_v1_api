package com.br.rianlucas.flowia_api.dtos.ocr;


public record OcrResponseDTO (
    Boolean success,
    String text
) {
    
}
