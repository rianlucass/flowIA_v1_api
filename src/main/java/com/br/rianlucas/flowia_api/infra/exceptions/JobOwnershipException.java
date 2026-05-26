package com.br.rianlucas.flowia_api.infra.exceptions;

public class JobOwnershipException extends RuntimeException {
    public JobOwnershipException() {
        super("You do not have permission to modify this job");
    }
}
