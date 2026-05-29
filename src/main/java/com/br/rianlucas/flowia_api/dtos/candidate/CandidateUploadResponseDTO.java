package com.br.rianlucas.flowia_api.dtos.candidate;

public record CandidateUploadResponseDTO(
        String candidateId,
        String jobId,
        String candidateName,
        String resumeText,
        Boolean processedByAi
) {
}
