package com.br.rianlucas.flowia_api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.dtos.job.CreateJobRequestDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobResponseDTO;
import com.br.rianlucas.flowia_api.repositories.JobRepository;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    public JobResponseDTO create(CreateJobRequestDTO data, User recruiter) {
        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setTitle(data.title());
        job.setDescription(data.description());
        job.setCompanyId(data.companyId());
        job.setModality(data.modality());
        job.setSalary(data.salary());
        job.setCity(data.city());
        job.setState(data.state());
        job.setStatus("OPEN");
        job.setCriteria(data.criteria());

        Job saved = jobRepository.save(job);

        return new JobResponseDTO(
                saved.getId(),
                saved.getRecruiter().getId(),
                saved.getCompanyId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getSalary(),
                saved.getModality(),
                saved.getCity(),
                saved.getState(),
                saved.getStatus(),
                saved.getCriteria(),
                saved.getCreatedAt()
        );
    }
}
