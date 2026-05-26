package com.br.rianlucas.flowia_api.infra.exceptions;

public class InvalidJobCriteriaException extends RuntimeException {
    public InvalidJobCriteriaException(String message) {
        super(message);
    }
}
