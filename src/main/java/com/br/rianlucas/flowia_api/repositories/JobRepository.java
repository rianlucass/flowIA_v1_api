package com.br.rianlucas.flowia_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.br.rianlucas.flowia_api.domain.job.Job;

public interface JobRepository extends JpaRepository<Job, String> {
    
}
