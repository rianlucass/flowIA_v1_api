package com.br.rianlucas.flowia_api.infra.exceptions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.br.rianlucas.flowia_api.domain.job.JobStatus;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void deveTratarEmailAlreadyExistsException() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("teste@email.com");
        ResponseEntity<Map<String, String>> response = handler.handleEmailAlreadyExists(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("teste@email.com"));
    }

    @Test
    void deveTratarUsernameAlreadyExistsException() {
        UsernameAlreadyExistsException ex = new UsernameAlreadyExistsException("joao123");
        ResponseEntity<Map<String, String>> response = handler.handleUsernameAlreadyExists(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("joao123"));
    }

    @Test
    void deveTratarJobNotFoundException() {
        JobNotFoundException ex = new JobNotFoundException("job-1");
        ResponseEntity<Map<String, String>> response = handler.handleJobNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("job-1"));
    }

    @Test
    void deveTratarCandidateNotFoundException() {
        CandidateNotFoundException ex = new CandidateNotFoundException("candidate-1");
        ResponseEntity<Map<String, String>> response = handler.handleCandidateNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("candidate-1"));
    }

    @Test
    void deveTratarInvalidJobStatusException() {
        InvalidJobStatusException ex = new InvalidJobStatusException("XPTO");
        ResponseEntity<Map<String, String>> response = handler.handleInvalidJobStatus(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("XPTO"));
    }

    @Test
    void deveTratarBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<Map<String, String>> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password", response.getBody().get("error"));
    }

    @Test
    void deveTratarLockedException() {
        LockedException ex = new LockedException("Account locked");
        ResponseEntity<Map<String, String>> response = handler.handleLocked(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Account is locked", response.getBody().get("error"));
    }

    @Test
    void deveTratarDisabledException() {
        DisabledException ex = new DisabledException("Account disabled");
        ResponseEntity<Map<String, String>> response = handler.handleDisabled(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Account is disabled", response.getBody().get("error"));
    }

    @Test
    void deveTratarMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "must not be blank");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must not be blank", response.getBody().get("email"));
    }

    @Test
    void deveTratarJobStatusTransitionException() {
        JobStatusTransitionException ex = new JobStatusTransitionException(JobStatus.DRAFT, JobStatus.CLOSED);
        ResponseEntity<Map<String, String>> response = handler.handleJobStatusTransition(ex);

        assertEquals(422, response.getStatusCode().value());
        assertTrue(response.getBody().get("error").contains("DRAFT"));
        assertTrue(response.getBody().get("error").contains("CLOSED"));
    }

    @Test
    void deveTratarJobOwnershipException() {
        JobOwnershipException ex = new JobOwnershipException();
        ResponseEntity<Map<String, String>> response = handler.handleJobOwnership(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("permission"));
    }

    @Test
    void deveTratarIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument value");
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid argument value", response.getBody().get("error"));
    }

    @Test
    void deveTratarInvalidJobCriteriaException() {
        InvalidJobCriteriaException ex = new InvalidJobCriteriaException("Weights must sum to 100");
        ResponseEntity<Map<String, String>> response = handler.handleInvalidJobCriteria(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Weights must sum to 100", response.getBody().get("error"));
    }

    @Test
    void deveTratarGenericException() {
        Exception ex = new Exception("Something went wrong");
        ResponseEntity<Map<String, String>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().get("error"));
    }
}
