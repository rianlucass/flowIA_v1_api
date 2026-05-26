package com.br.rianlucas.flowia_api.infra.exceptions;

import com.br.rianlucas.flowia_api.domain.job.JobStatus;

public class JobStatusTransitionException extends RuntimeException {
    public JobStatusTransitionException(JobStatus from, JobStatus to) {
        super("Invalid status transition from " + from + " to " + to);
    }
}
