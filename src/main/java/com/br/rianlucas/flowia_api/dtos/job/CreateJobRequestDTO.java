package com.br.rianlucas.flowia_api.dtos.job;

import com.br.rianlucas.flowia_api.domain.job.JobCriteria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequestDTO(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String companyId,
        @NotBlank String modality,
        String salary,
        String city,
        String state,
        @NotNull JobCriteria criteria
) {
}
