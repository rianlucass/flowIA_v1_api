package com.br.rianlucas.flowia_api.dtos.webhook;

import com.br.rianlucas.flowia_api.domain.job.JobCriteria;

public record N8NWebhookPayloadDTO(
    String candidateId,
    String jobId,
    String resumeUrl,
    String resumeText,
    JobCriteria criteria
) {}
