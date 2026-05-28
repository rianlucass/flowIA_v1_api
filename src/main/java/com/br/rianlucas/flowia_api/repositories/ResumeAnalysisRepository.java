package com.br.rianlucas.flowia_api.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.rianlucas.flowia_api.domain.analysis.ResumeAnalysis;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, String> {

	Optional<ResumeAnalysis> findFirstByJobId(String jobId);
}
