package com.br.rianlucas.flowia_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.rianlucas.flowia_api.domain.candidates.Candidate;

public interface CandidateRepository extends JpaRepository<Candidate, String> {

    List<Candidate> findByJobId(String jobId);

}
