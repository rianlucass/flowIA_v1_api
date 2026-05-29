package com.br.rianlucas.flowia_api.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.br.rianlucas.flowia_api.domain.candidates.Candidate;
import com.br.rianlucas.flowia_api.domain.candidates.CandidateStatus;
import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.dtos.candidate.ApplyJobResponseDTO;
import com.br.rianlucas.flowia_api.dtos.candidate.CandidateResponseDTO;
import com.br.rianlucas.flowia_api.dtos.candidate.CreateCandidateRequestDTO;
import com.br.rianlucas.flowia_api.dtos.webhook.N8NWebhookPayloadDTO;
import com.br.rianlucas.flowia_api.infra.exceptions.JobNotFoundException;
import com.br.rianlucas.flowia_api.repositories.CandidateRepository;
import com.br.rianlucas.flowia_api.repositories.JobRepository;

import jakarta.transaction.Transactional;

@Service
public class CandidatesService {

    private static final Logger log = LoggerFactory.getLogger(CandidatesService.class);

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private N8NWebhookService n8nWebhookService;

    public List<CandidateResponseDTO> getCandidatesByJobId(String jobId) {
        return candidateRepository.findByJobId(jobId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ApplyJobResponseDTO apply(String jobId, MultipartFile file) {
        if (!isPdf(file)) {
            throw new IllegalArgumentException("Apenas arquivos PDF são aceitos.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        // Cria o stub do candidato para obter o ID gerado
        Candidate candidate = new Candidate();
        candidate.setJob(job);
        candidate.setStatus(CandidateStatus.RECEIVED);
        candidate.setProcessedByAi(false);
        candidate = candidateRepository.save(candidate);

        // Salva o PDF usando recruiterId/jobId/candidateId como estrutura
        String recruiterId = job.getRecruiter().getId();
        String resumeUrl = fileStorageService.storeResume(file, recruiterId, jobId, candidate.getId());
        candidate.setResumeUrl(resumeUrl);
        candidateRepository.save(candidate);

        // OCR: extrai o texto do currículo
        String resumeText = null;
        try {
            resumeText = ocrService.extractText(file);
            candidate.setResumeText(resumeText);
            candidate.setStatus(CandidateStatus.PROCESSING);
            candidateRepository.save(candidate);
        } catch (Exception e) {
            log.error("OCR falhou para candidateId={}: {}", candidate.getId(), e.getMessage());
        }

        // Notifica o N8N com os dados do candidato e os critérios da vaga
        n8nWebhookService.notifyApplication(new N8NWebhookPayloadDTO(
                candidate.getId(),
                jobId,
                resumeUrl,
                resumeText,
                job.getCriteria()
        ));

        return new ApplyJobResponseDTO(candidate.getId(), jobId, resumeUrl, candidate.getStatus());
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.equals("application/pdf");
    }

    @Transactional
    public CandidateResponseDTO create(CreateCandidateRequestDTO data) {
        Job job = jobRepository.findById(data.jobId())
                .orElseThrow(() -> new JobNotFoundException(data.jobId()));

        Candidate candidate = new Candidate();
        candidate.setJob(job);
        candidate.setName(data.name());
        candidate.setEmail(data.email());
        candidate.setPhone(data.phone());
        candidate.setCity(data.city());
        candidate.setState(data.state());
        candidate.setLinkedinUrl(data.linkedinUrl());
        candidate.setPortfolioUrl(data.portfolioUrl());
        candidate.setResumeUrl(data.resumeUrl());
        candidate.setResumeText(data.resumeText());
        candidate.setStatus(data.status() != null ? data.status() : CandidateStatus.RECEIVED);
        candidate.setProcessedByAi(data.processedByAi() != null ? data.processedByAi() : false);

        return toDTO(candidateRepository.save(candidate));
    }

    @Transactional
    public CandidateResponseDTO createFromUpload(String jobId, String name, String email, String resumeText) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        Candidate candidate = new Candidate();
        candidate.setJob(job);
        candidate.setName(name);
        candidate.setEmail(email);
        candidate.setResumeText(resumeText);
        candidate.setStatus(CandidateStatus.RECEIVED);
        candidate.setProcessedByAi(false);

        return toDTO(candidateRepository.save(candidate));
    }

    private CandidateResponseDTO toDTO(Candidate candidate) {
        return new CandidateResponseDTO(
                candidate.getId(),
                candidate.getJob().getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getPhone(),
                candidate.getCity(),
                candidate.getState(),
                candidate.getLinkedinUrl(),
                candidate.getPortfolioUrl(),
                candidate.getResumeUrl(),
                candidate.getResumeText(),
                candidate.getStatus(),
                candidate.getProcessedByAi(),
                candidate.getAnalysisOutdated(),
                candidate.getCreatedAt()
        );
    }
}
