package com.br.rianlucas.flowia_api.dtos.candidate;

import com.br.rianlucas.flowia_api.domain.candidates.CandidateStatus;

public record ApplyJobResponseDTO(
        String candidateId,
        String jobId,
        String resumeUrl,
        CandidateStatus status
) {
}
