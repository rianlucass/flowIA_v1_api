package com.br.rianlucas.flowia_api.infra.exceptions;

public class CandidateNotFoundException extends RuntimeException {
    public CandidateNotFoundException(String id) {
        super("Candidate not found with id: " + id);
    }
}
