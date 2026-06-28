package com.br.rianlucas.flowia_api.dtos.job;

import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.domain.job.JobStatus;

/**
 * DTO para resposta pública de uma vaga.
 * Usado pelo endpoint público /jobs/{id}/public que não requer autenticação.
 * 
 * Expõe apenas informações públicas da vaga, sem dados sensíveis como:
 * - recruiterId
 * - companyId
 * - criteria (critérios de análise da IA)
 */
public record JobPublicResponseDTO(
    String id,
    String title,
    String description,
    String modality,
    String salary,
    String city,
    String state,
    JobStatus status
) {
    /**
     * Converte uma entidade Job para JobPublicResponseDTO.
     * 
     * @param job Entidade Job a ser convertida
     * @return DTO com apenas dados públicos da vaga
     */
    public static JobPublicResponseDTO fromEntity(Job job) {
        return new JobPublicResponseDTO(
            job.getId(),
            job.getTitle(),
            job.getDescription(),
            job.getModality(),
            job.getSalary(),
            job.getCity(),
            job.getState(),
            job.getStatus()
        );
    }
}
