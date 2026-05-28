package com.br.rianlucas.flowia_api.dtos.candidate;

import java.time.LocalDateTime;

import com.br.rianlucas.flowia_api.domain.candidates.CandidateStatus;

public record CandidateResponseDTO(

    String id,

    String jobId,

    String name,

    String email,

    String phone,

    String city,

    String state,

    String linkedinUrl,

    String portfolioUrl,

    String resumeUrl,

    String resumeText,

    CandidateStatus status,

    Boolean processedByAi,

    Boolean analysisOutdated,

    LocalDateTime createdAt

) {

}
