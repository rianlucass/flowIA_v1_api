package com.br.rianlucas.flowia_api.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.br.rianlucas.flowia_api.domain.job.JobCriteria;
import com.br.rianlucas.flowia_api.dtos.webhook.N8NWebhookPayloadDTO;

import reactor.core.publisher.Mono;

class N8NWebhookServiceTest {

    private N8NWebhookService n8nWebhookService;

    private WebClient webClient;
    private WebClient.RequestBodyUriSpec uriSpec;
    private WebClient.RequestBodySpec bodySpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;

    private N8NWebhookPayloadDTO payload;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        n8nWebhookService = new N8NWebhookService("http://localhost:5678/webhook/apply");

        webClient = mock(WebClient.class);
        uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        ReflectionTestUtils.setField(n8nWebhookService, "webClient", webClient);

        JobCriteria criteria = new JobCriteria();
        payload = new N8NWebhookPayloadDTO(
                "candidate-1",
                "job-1",
                "uploads/resume.pdf",
                "Texto extraido",
                criteria);
    }

    @Test
    void deveNotificarComSucesso() {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(payload)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.just(ResponseEntity.ok().<Void>build()));

        n8nWebhookService.notifyApplication(payload);

        verify(webClient).post();
        verify(uriSpec).contentType(MediaType.APPLICATION_JSON);
        verify(headersSpec).retrieve();
        verify(responseSpec).toBodilessEntity();
    }

    @Test
    void deveLogarErroQuandoWebhookFalha() {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(payload)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        n8nWebhookService.notifyApplication(payload);

        verify(responseSpec).toBodilessEntity();
    }

    @Test
    void deveEnviarPayloadCorreto() {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(payload)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.just(new ResponseEntity<>(HttpStatus.OK)));

        n8nWebhookService.notifyApplication(payload);

        verify(bodySpec).bodyValue(payload);
    }

    @Test
    void naoDeveLancarExcecaoQuandoWebhookFalha() {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.error(new RuntimeException("Erro qualquer")));

        assertDoesNotThrow(() -> n8nWebhookService.notifyApplication(payload));
    }
}
