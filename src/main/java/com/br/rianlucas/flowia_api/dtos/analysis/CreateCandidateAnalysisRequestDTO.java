package com.br.rianlucas.flowia_api.dtos.analysis;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.br.rianlucas.flowia_api.domain.analysis.AnalysisStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCandidateAnalysisRequestDTO(

    @NotBlank String candidateId,

    @NotBlank String jobId,

    // Dados de perfil extraídos pelo AI — atualizam o Candidate
    String candidateName,
    String email,
    String phone,
    String city,
    String state,
    String linkedinUrl,
    String portfolioUrl,

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

    List<String> eliminationReasons,

    String aiModel,

    String promptVersion,

    Boolean outdated

) {}
