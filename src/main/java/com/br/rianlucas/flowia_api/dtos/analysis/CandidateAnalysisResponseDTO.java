package com.br.rianlucas.flowia_api.dtos.analysis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.br.rianlucas.flowia_api.domain.analysis.AnalysisStatus;

public record CandidateAnalysisResponseDTO(
    String id,
    String candidateId,
    String jobId,
    BigDecimal finalScore,
    BigDecimal activitiesScore,
    BigDecimal experienceScore,
    BigDecimal educationScore,
    BigDecimal locationScore,
    BigDecimal stabilityScore,
    AnalysisStatus status,
    Map<String, Object> strengths,
    Map<String, Object> attentionPoints,
    Map<String, Object> missingInformation,
    Map<String, Object> interviewQuestions,
    String recommendation,
    Map<String, Object> validations,
    Map<String, Object> weightsUsed,
    List<String> eliminationReasons,
    String aiModel,
    String promptVersion,
    Boolean outdated,
    LocalDateTime createdAt
) {}
