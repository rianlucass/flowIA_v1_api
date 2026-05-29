package com.br.rianlucas.flowia_api.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.br.rianlucas.flowia_api.dtos.webhook.N8NWebhookPayloadDTO;

@Service
public class N8NWebhookService {

    private static final Logger log = LoggerFactory.getLogger(N8NWebhookService.class);

    private final WebClient webClient;

    public N8NWebhookService(@Value("${n8n.webhook.url}") String webhookUrl) {
        this.webClient = WebClient.builder().baseUrl(webhookUrl).build();
    }

    public void notifyApplication(N8NWebhookPayloadDTO payload) {
        try {
            webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Webhook N8N enviado com sucesso para candidateId={}", payload.candidateId());
        } catch (Exception e) {
            log.error("Falha ao notificar N8N para candidateId={}: {}", payload.candidateId(), e.getMessage());
        }
    }
}
