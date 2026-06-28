package com.br.rianlucas.flowia_api.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.domain.job.JobCriteria;
import com.br.rianlucas.flowia_api.domain.job.JobStatus;
import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.dtos.job.CreateJobRequestDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobPublicResponseDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobResponseDTO;
import com.br.rianlucas.flowia_api.dtos.job.UpdateJobRequestDTO;
import com.br.rianlucas.flowia_api.infra.exceptions.InvalidJobCriteriaException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobNotFoundException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobOwnershipException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobStatusTransitionException;
import com.br.rianlucas.flowia_api.repositories.JobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;


    public List<JobResponseDTO> getByRecruiter(User recruiter) {
        return jobRepository.findByRecruiterId(recruiter.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public JobResponseDTO update(String id, UpdateJobRequestDTO data, User recruiter) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new JobOwnershipException();
        }

        validateUpdateRules(job, data);
        applyPartialUpdate(job, data);

        return toDTO(jobRepository.save(job));
    }

    private void validateUpdateRules(Job job, UpdateJobRequestDTO data) {
        JobStatus currentStatus = job.getStatus();

        if (data.status() != null && !data.status().equals(currentStatus)) {
            validateStatusTransition(currentStatus, data.status());
        }

        if (data.criteria() != null) {
            if (currentStatus == JobStatus.CLOSED) {
                throw new InvalidJobCriteriaException("Cannot change criteria of a CLOSED job");
            }
            validateWeightSum(data.criteria().getWeights());
        }

        if (currentStatus == JobStatus.CLOSED && data.modality() != null) {
            throw new InvalidJobCriteriaException("Cannot change modality of a CLOSED job");
        }
    }

    private void applyPartialUpdate(Job job, UpdateJobRequestDTO data) {
        if (data.title() != null)       job.setTitle(data.title());
        if (data.description() != null) job.setDescription(data.description());
        if (data.modality() != null)    job.setModality(data.modality());
        if (data.salary() != null)      job.setSalary(data.salary());
        if (data.city() != null)        job.setCity(data.city());
        if (data.state() != null)       job.setState(data.state());
        if (data.status() != null)      job.setStatus(data.status());
        if (data.criteria() != null) {
            job.setCriteria(data.criteria());
            job.setCriteriaUpdatedAt(LocalDateTime.now());
        }
    }

    private JobResponseDTO toDTO(Job job) {
        return new JobResponseDTO(
                job.getId(),
                job.getRecruiter().getId(),
                job.getTitle(),
                job.getDescription(),
                job.getSalary(),
                job.getModality(),
                job.getCity(),
                job.getState(),
                job.getStatus(),
                job.getCriteria(),
                job.getCreatedAt(),
                job.getCriteriaUpdatedAt()
        );
    }

    private void validateStatusTransition(JobStatus from, JobStatus to) {
        boolean valid = switch (from) {
            case DRAFT  -> to == JobStatus.OPEN;
            case OPEN   -> to == JobStatus.CLOSED || to == JobStatus.PAUSED;
            case PAUSED -> to == JobStatus.OPEN   || to == JobStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new JobStatusTransitionException(from, to);
        }
    }

    private void validateWeightSum(JobCriteria.WeightCriteria weights) {
        if (weights == null) return;
        int sum = Optional.ofNullable(weights.getActivities()).orElse(0)
                + Optional.ofNullable(weights.getExperience()).orElse(0)
                + Optional.ofNullable(weights.getEducation()).orElse(0)
                + Optional.ofNullable(weights.getLocation()).orElse(0)
                + Optional.ofNullable(weights.getStability()).orElse(0);
        if (sum != 100) {
            throw new InvalidJobCriteriaException(
                    "Criteria weights must sum to 100, but got: " + sum);
        }
    }


    @Transactional
    public JobResponseDTO create(CreateJobRequestDTO data, User recruiter) {
        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setTitle(data.title());
        job.setDescription(data.description());
        job.setModality(data.modality());
        job.setSalary(data.salary());
        job.setCity(data.city());
        job.setState(data.state());
        job.setStatus(JobStatus.OPEN);
        job.setCriteria(data.criteria());

        return toDTO(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public JobResponseDTO getById(String id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
        return toDTO(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDTO> getAll() {
        return jobRepository.findAll().stream().map(this::toDTO).toList();
    }

    /**
     * Busca informações públicas de uma vaga (sem autenticação).
     * Usado pela página pública de candidatura.
     * 
     * @param jobId ID da vaga
     * @return Dados públicos da vaga
     * @throws JobNotFoundException se a vaga não existir
     */
    @Transactional(readOnly = true)
    public JobPublicResponseDTO getJobPublic(String jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
        
        return JobPublicResponseDTO.fromEntity(job);
    }

}
