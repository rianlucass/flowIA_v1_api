package com.br.rianlucas.flowia_api.dtos.job;

import com.br.rianlucas.flowia_api.domain.job.JobCriteria;
import com.br.rianlucas.flowia_api.domain.job.JobStatus;

public record UpdateJobRequestDTO(
    String title,
    String description,
    String modality,
    String salary,
    String city,
    String state,
    JobStatus status,
    JobCriteria criteria
) {}
