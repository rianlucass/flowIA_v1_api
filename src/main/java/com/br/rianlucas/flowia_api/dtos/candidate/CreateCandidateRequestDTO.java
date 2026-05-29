package com.br.rianlucas.flowia_api.dtos.candidate;

import com.br.rianlucas.flowia_api.domain.candidates.CandidateStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCandidateRequestDTO(

        @NotBlank
        String jobId,

        String name,

        @Email
        String email,

        String phone,
        String city,
        String state,
        String linkedinUrl,
        String portfolioUrl,
        String resumeUrl,
        String resumeText,
        CandidateStatus status,
        Boolean processedByAi
) {
}
