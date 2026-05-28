package com.br.rianlucas.flowia_api.dtos.analysis;

import java.math.BigDecimal;
import java.util.Map;

import com.br.rianlucas.flowia_api.domain.analysis.AnalysisStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateResumeAnalysisRequestDTO(

    @NotBlank String candidateId,

    @NotBlank String jobId,

    BigDecimal finalScore,

    BigDecimal activitiesScore,

    BigDecimal experienceScore,

    BigDecimal educationScore,

    BigDecimal locationScore,

    BigDecimal stabilityScore,

    @NotNull AnalysisStatus status,

    Map<String, Object> strengths,

    Map<String, Object> attentionPoints,

    Map<String, Object> missingInformation,

    Map<String, Object> interviewQuestions,

    String recommendation,

    Map<String, Object> validations,

    Map<String, Object> weightsUsed,

    String aiModel,

    String promptVersion,

    Boolean outdated

) {}
