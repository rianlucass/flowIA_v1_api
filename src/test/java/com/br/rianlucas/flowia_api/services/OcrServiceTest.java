package com.br.rianlucas.flowia_api.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.br.rianlucas.flowia_api.dtos.ocr.OcrResponseDTO;
import com.br.rianlucas.flowia_api.infra.exceptions.OcrServiceException;

import reactor.core.publisher.Mono;

class OcrServiceTest {

    private OcrService ocrService;

    private WebClient webClient;

    private WebClient.RequestBodyUriSpec uriSpec;
    private WebClient.RequestBodySpec bodySpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;

    private MockMultipartFile pdfFile;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ocrService = new OcrService("http://localhost:5000");

        webClient = mock(WebClient.class);
        uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        ReflectionTestUtils.setField(ocrService, "webClient", webClient);

        pdfFile = new MockMultipartFile(
                "file",
                "curriculo.pdf",
                "application/pdf",
                "conteudo do pdf".getBytes());
    }

    @Test
    void deveExtrairTextoComSucesso() {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/ocr")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OcrResponseDTO.class))
                .thenReturn(Mono.just(new OcrResponseDTO(true, "Texto extraido do curriculo")));

        String result = ocrService.extractText(pdfFile);

        assertNotNull(result);
        assertEquals("Texto extraido do curriculo", result);

        verify(webClient).post();
        verify(uriSpec).uri("/ocr");
        verify(bodySpec).contentType(MediaType.MULTIPART_FORM_DATA);
        verify(bodySpec).bodyValue(any());
        verify(headersSpec).retrieve();
        verify(responseSpec).bodyToMono(OcrResponseDTO.class);
    }

    @Test
    void deveLancarExcecaoQuandoRespostaNula() {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/ocr")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OcrResponseDTO.class)).thenReturn(Mono.empty());

        OcrServiceException exception = assertThrows(OcrServiceException.class,
                () -> ocrService.extractText(pdfFile));

        assertEquals("OCR service returned null response", exception.getMessage());
    }

    @Test
    void devePropagarExcecaoQuandoWebClientFalha() {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/ocr")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OcrResponseDTO.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ocrService.extractText(pdfFile));

        assertEquals("Connection refused", exception.getMessage());
    }

    @Test
    void deveEnviarArquivoCorretoParaOcr() {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/ocr")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OcrResponseDTO.class))
                .thenReturn(Mono.just(new OcrResponseDTO(true, "ok")));

        ocrService.extractText(pdfFile);

        verify(bodySpec).contentType(MediaType.MULTIPART_FORM_DATA);
    }
}
