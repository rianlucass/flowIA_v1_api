package com.br.rianlucas.flowia_api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.br.rianlucas.flowia_api.dtos.ocr.OcrResponseDTO;

@Service
public class OcrService {

    private final WebClient webClient;

    public OcrService(@Value("${ocr.service.url}") String ocrServiceUrl) {
        this.webClient = WebClient.builder().baseUrl(ocrServiceUrl).build();
    }

    public String extractText(MultipartFile file) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", file.getResource());

        OcrResponseDTO response = webClient.post()
                .uri("/ocr")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body.build())
                .retrieve()
                .bodyToMono(OcrResponseDTO.class)
                .block();

        return response.text();
    }
}
