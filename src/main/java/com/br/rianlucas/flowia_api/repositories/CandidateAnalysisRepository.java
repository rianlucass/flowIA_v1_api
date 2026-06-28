package com.br.rianlucas.flowia_api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.br.rianlucas.flowia_api.domain.analysis.CandidateAnalysis;

public interface CandidateAnalysisRepository extends JpaRepository<CandidateAnalysis, String> {

    @Query("SELECT ca FROM CandidateAnalysis ca JOIN FETCH ca.candidate WHERE ca.job.id = :jobId")
    Optional<CandidateAnalysis> findFirstByJobId(@Param("jobId") String jobId);

    @Query("SELECT ca FROM CandidateAnalysis ca JOIN FETCH ca.candidate WHERE ca.job.id = :jobId")
    List<CandidateAnalysis> findByJobId(@Param("jobId") String jobId);

    @Query("SELECT ca FROM CandidateAnalysis ca JOIN FETCH ca.candidate WHERE ca.candidate.id = :candidateId")
    List<CandidateAnalysis> findByCandidateId(@Param("candidateId") String candidateId);
}
