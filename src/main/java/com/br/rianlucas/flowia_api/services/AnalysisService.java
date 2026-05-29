package com.br.rianlucas.flowia_api.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.br.rianlucas.flowia_api.domain.analysis.AnalysisStatus;
import com.br.rianlucas.flowia_api.domain.analysis.CandidateAnalysis;
import com.br.rianlucas.flowia_api.domain.candidates.Candidate;
import com.br.rianlucas.flowia_api.domain.candidates.CandidateStatus;
import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.dtos.analysis.CandidateAnalysisResponseDTO;
import com.br.rianlucas.flowia_api.dtos.analysis.CreateCandidateAnalysisRequestDTO;
import com.br.rianlucas.flowia_api.infra.exceptions.CandidateNotFoundException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobNotFoundException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobOwnershipException;
import com.br.rianlucas.flowia_api.repositories.CandidateAnalysisRepository;
import com.br.rianlucas.flowia_api.repositories.CandidateRepository;
import com.br.rianlucas.flowia_api.repositories.JobRepository;

import jakarta.transaction.Transactional;

@Service
public class AnalysisService {

    @Autowired
    private CandidateAnalysisRepository candidateAnalysisRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Transactional
    public CandidateAnalysisResponseDTO create(CreateCandidateAnalysisRequestDTO data) {
        Candidate candidate = candidateRepository.findById(data.candidateId())
                .orElseThrow(() -> new CandidateNotFoundException(data.candidateId()));

        Job job = jobRepository.findById(data.jobId())
                .orElseThrow(() -> new JobNotFoundException(data.jobId()));

        // Atualiza perfil do candidato com dados extraídos pelo AI
        enrichCandidate(candidate, data);
        candidateRepository.save(candidate);

        CandidateAnalysis analysis = new CandidateAnalysis();
        analysis.setCandidate(candidate);
        analysis.setJob(job);
        analysis.setFinalScore(data.finalScore());
        analysis.setActivitiesScore(data.activitiesScore());
        analysis.setExperienceScore(data.experienceScore());
        analysis.setEducationScore(data.educationScore());
        analysis.setLocationScore(data.locationScore());
        analysis.setStabilityScore(data.stabilityScore());
        analysis.setStatus(data.status());
        analysis.setStrengths(data.strengths());
        analysis.setAttentionPoints(data.attentionPoints());
        analysis.setMissingInformation(data.missingInformation());
        analysis.setInterviewQuestions(data.interviewQuestions());
        analysis.setRecommendation(data.recommendation());
        analysis.setValidations(data.validations());
        analysis.setWeightsUsed(data.weightsUsed());
        analysis.setEliminationReasons(data.eliminationReasons());
        analysis.setAiModel(data.aiModel());
        analysis.setPromptVersion(data.promptVersion());
        analysis.setOutdated(data.outdated() != null ? data.outdated() : false);

        CandidateAnalysis saved = candidateAnalysisRepository.save(analysis);
        return toDTO(saved);
    }

    // Preenche os campos do candidato com dados extraídos pelo AI.
    // Ignora valores nulos ou o placeholder "informação não encontrada".
    private void enrichCandidate(Candidate candidate, CreateCandidateAnalysisRequestDTO data) {
        if (isPresent(data.candidateName()))  candidate.setName(data.candidateName());
        if (isPresent(data.email()))          candidate.setEmail(data.email());
        if (isPresent(data.phone()))          candidate.setPhone(data.phone());
        if (isPresent(data.city()))           candidate.setCity(data.city());
        if (isPresent(data.state()))          candidate.setState(data.state());
        if (isPresent(data.linkedinUrl()))    candidate.setLinkedinUrl(data.linkedinUrl());
        if (isPresent(data.portfolioUrl()))   candidate.setPortfolioUrl(data.portfolioUrl());

        candidate.setProcessedByAi(true);

        // Sincroniza status do candidato com o resultado da análise
        if (data.status() == AnalysisStatus.APPROVED)   candidate.setStatus(CandidateStatus.APPROVED);
        else if (data.status() == AnalysisStatus.REJECTED) candidate.setStatus(CandidateStatus.REJECTED);
        else if (data.status() == AnalysisStatus.REVIEW)   candidate.setStatus(CandidateStatus.REVIEW);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("informação não encontrada");
    }

    public CandidateAnalysisResponseDTO getAnalysisByJobId(String jobId) {
        var analysis = candidateAnalysisRepository.findFirstByJobId(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        return toDTO(analysis);
    }

    public List<CandidateAnalysisResponseDTO> getAllAnalysisByJobId(String jobId, User requester) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!job.getRecruiter().getId().equals(requester.getId())) {
            throw new JobOwnershipException();
        }

        return candidateAnalysisRepository.findByJobId(jobId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private CandidateAnalysisResponseDTO toDTO(CandidateAnalysis analysis) {
        return new CandidateAnalysisResponseDTO(
                analysis.getId(),
                analysis.getCandidate().getId(),
                analysis.getJob().getId(),
                analysis.getFinalScore(),
                analysis.getActivitiesScore(),
                analysis.getExperienceScore(),
                analysis.getEducationScore(),
                analysis.getLocationScore(),
                analysis.getStabilityScore(),
                analysis.getStatus(),
                analysis.getStrengths(),
                analysis.getAttentionPoints(),
                analysis.getMissingInformation(),
                analysis.getInterviewQuestions(),
                analysis.getRecommendation(),
                analysis.getValidations(),
                analysis.getWeightsUsed(),
                analysis.getEliminationReasons(),
                analysis.getAiModel(),
                analysis.getPromptVersion(),
                analysis.getOutdated(),
                analysis.getCreatedAt()
        );
    }
}
