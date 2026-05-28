package com.br.rianlucas.flowia_api.infra.exceptions;

public class InvalidJobStatusException extends RuntimeException {
    public InvalidJobStatusException(String status) {
        super("Invalid job status: " + status);
    }
}
