package com.br.rianlucas.flowia_api.infra.exceptions;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String id) {
        super("Job not found with id: " + id);
    }
}
