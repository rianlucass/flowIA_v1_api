package com.br.rianlucas.flowia_api.infra.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email already in use: " + email);
    }
}
