package com.br.rianlucas.flowia_api.dtos.job;

import java.time.LocalDateTime;

import com.br.rianlucas.flowia_api.domain.job.JobCriteria;
import com.br.rianlucas.flowia_api.domain.job.JobStatus;

public record JobResponseDTO(

    String id,

    String recruiterId,

    String companyId,

    String title,

    String description,

    String salary,

    String modality,

    String city,

    String state,

    JobStatus status,

    JobCriteria criteria,

    LocalDateTime createdAt,

    LocalDateTime criteriaUpdatedAt

) {
    
}
