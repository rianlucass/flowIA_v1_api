package com.br.rianlucas.flowia_api.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.br.rianlucas.flowia_api.dtos.candidate.CandidateResponseDTO;
import com.br.rianlucas.flowia_api.repositories.CandidateRepository;

@Service
public class CandidatesService {
    
    @Autowired
    private CandidateRepository candidateRepository;

    public List<CandidateResponseDTO> getCandidatesByJobId(String jobId) {
        return candidateRepository.findByJobId(jobId)
                .stream()
                .map(candidate -> new CandidateResponseDTO(
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
                ))
                .toList();
    }

    

}
