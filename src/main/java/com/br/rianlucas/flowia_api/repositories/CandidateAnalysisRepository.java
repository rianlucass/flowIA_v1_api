package com.br.rianlucas.flowia_api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.rianlucas.flowia_api.domain.analysis.CandidateAnalysis;

public interface CandidateAnalysisRepository extends JpaRepository<CandidateAnalysis, String> {

    Optional<CandidateAnalysis> findFirstByJobId(String jobId);

    List<CandidateAnalysis> findByJobId(String jobId);

    List<CandidateAnalysis> findByCandidateId(String candidateId);
}
