package com.br.rianlucas.flowia_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.rianlucas.flowia_api.domain.job.Job;

public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByRecruiterId(String recruiterId);
}
