package com.br.rianlucas.flowia_api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.br.rianlucas.flowia_api.domain.analysis.ResumeAnalysis;
import com.br.rianlucas.flowia_api.domain.candidates.Candidate;
import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.dtos.analysis.CreateResumeAnalysisRequestDTO;
import com.br.rianlucas.flowia_api.dtos.analysis.ResumeAnalysisResponseDTO;
import com.br.rianlucas.flowia_api.repositories.CandidateRepository;
import com.br.rianlucas.flowia_api.repositories.JobRepository;
import com.br.rianlucas.flowia_api.repositories.ResumeAnalysisRepository;

import jakarta.transaction.Transactional;

@Service
public class AnalysisService {

    @Autowired
    private ResumeAnalysisRepository resumeAnalysisRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Transactional
    public ResumeAnalysisResponseDTO create(CreateResumeAnalysisRequestDTO data) {
        Candidate candidate = candidateRepository.findById(data.candidateId())
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + data.candidateId()));

        Job job = jobRepository.findById(data.jobId())
                .orElseThrow(() -> new RuntimeException("Job not found: " + data.jobId()));

        ResumeAnalysis analysis = new ResumeAnalysis();
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
        analysis.setAiModel(data.aiModel());
        analysis.setPromptVersion(data.promptVersion());
        analysis.setOutdated(data.outdated() != null ? data.outdated() : false);

        ResumeAnalysis saved = resumeAnalysisRepository.save(analysis);
        return toDTO(saved);
    }

    public ResumeAnalysisResponseDTO getCandidatesAnalysisByJobId(String jobId) {
        var analysis = resumeAnalysisRepository.findFirstByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("No analysis found for job ID: " + jobId));

        return toDTO(analysis);
    }

    private ResumeAnalysisResponseDTO toDTO(ResumeAnalysis analysis) {
        return new ResumeAnalysisResponseDTO(
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
                analysis.getAiModel(),
                analysis.getPromptVersion(),
                analysis.getOutdated(),
                analysis.getCreatedAt()
        );
    }
}
