package com.br.rianlucas.flowia_api.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.br.rianlucas.flowia_api.domain.job.JobCriteria;
import com.br.rianlucas.flowia_api.domain.job.JobStatus;
import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.domain.user.UserRole;
import com.br.rianlucas.flowia_api.dtos.job.CreateJobRequestDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobPublicResponseDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobResponseDTO;
import com.br.rianlucas.flowia_api.dtos.job.UpdateJobRequestDTO;
import com.br.rianlucas.flowia_api.services.JobService;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private JobService jobService;

    @InjectMocks
    private JobController jobController;

    private User recruiter;
    private JobResponseDTO jobResponse;
    private JobCriteria criteria;

    @BeforeEach
    void setUp() {
        recruiter = new User("recrutador@email.com", "123456", "recrutador", "Recrutador Teste", UserRole.USER);
        recruiter.setId("recruiter-1");

        criteria = new JobCriteria();

        jobResponse = new JobResponseDTO(
                "job-1",
                "recruiter-1",
                "Desenvolvedor Java",
                "Descricao da vaga",
                "5000",
                "Presencial",
                "Sao Paulo",
                "SP",
                JobStatus.OPEN,
                criteria,
                LocalDateTime.now(),
                null);
    }

    @Test
    void deveCriarVagaComSucesso() {
        CreateJobRequestDTO dto = new CreateJobRequestDTO(
                "Desenvolvedor Java", "Descricao", "Presencial", "5000", "Sao Paulo", "SP", criteria);
        when(jobService.create(dto, recruiter)).thenReturn(jobResponse);

        ResponseEntity<JobResponseDTO> response = jobController.create(dto, recruiter);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("job-1", response.getBody().id());
        assertEquals("Desenvolvedor Java", response.getBody().title());

        verify(jobService).create(dto, recruiter);
    }

    @Test
    void deveListarMinhasVagas() {
        when(jobService.getByRecruiter(recruiter)).thenReturn(List.of(jobResponse));

        ResponseEntity<List<JobResponseDTO>> response = jobController.getMyJobs(recruiter);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("job-1", response.getBody().get(0).id());

        verify(jobService).getByRecruiter(recruiter);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoTemVagas() {
        when(jobService.getByRecruiter(recruiter)).thenReturn(List.of());

        ResponseEntity<List<JobResponseDTO>> response = jobController.getMyJobs(recruiter);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void deveBuscarVagaPorId() {
        when(jobService.getById("job-1")).thenReturn(jobResponse);

        ResponseEntity<JobResponseDTO> response = jobController.getById("job-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("job-1", response.getBody().id());
        assertEquals("Desenvolvedor Java", response.getBody().title());

        verify(jobService).getById("job-1");
    }

    @Test
    void deveAtualizarVaga() {
        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                "Novo Titulo", null, null, null, null, null, null, null);
        JobResponseDTO updatedJob = new JobResponseDTO(
                "job-1", "recruiter-1", "Novo Titulo", "Descricao da vaga",
                "5000", "Presencial", "Sao Paulo", "SP",
                JobStatus.OPEN, criteria, LocalDateTime.now(), LocalDateTime.now());
        when(jobService.update("job-1", dto, recruiter)).thenReturn(updatedJob);

        ResponseEntity<JobResponseDTO> response = jobController.update("job-1", dto, recruiter);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Novo Titulo", response.getBody().title());

        verify(jobService).update("job-1", dto, recruiter);
    }

    @Test
    void deveAtualizarVagaComStatus() {
        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.CLOSED, null);
        JobResponseDTO closedJob = new JobResponseDTO(
                "job-1", "recruiter-1", "Desenvolvedor Java", "Descricao da vaga",
                "5000", "Presencial", "Sao Paulo", "SP",
                JobStatus.CLOSED, criteria, LocalDateTime.now(), LocalDateTime.now());
        when(jobService.update("job-1", dto, recruiter)).thenReturn(closedJob);

        ResponseEntity<JobResponseDTO> response = jobController.update("job-1", dto, recruiter);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(JobStatus.CLOSED, response.getBody().status());
    }

    @Test
    void deveBuscarVagaPublica() {
        JobPublicResponseDTO publicJob = new JobPublicResponseDTO(
                "job-1", "Desenvolvedor Java", "Descricao",
                "Presencial", "5000", "Sao Paulo", "SP", JobStatus.OPEN);
        when(jobService.getJobPublic("job-1")).thenReturn(publicJob);

        ResponseEntity<JobPublicResponseDTO> response = jobController.getJobPublic("job-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("job-1", response.getBody().id());
        assertEquals("Desenvolvedor Java", response.getBody().title());
        assertEquals("Sao Paulo", response.getBody().city());

        verify(jobService).getJobPublic("job-1");
    }
}
