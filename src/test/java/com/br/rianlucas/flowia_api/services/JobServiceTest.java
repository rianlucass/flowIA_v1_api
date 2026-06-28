package com.br.rianlucas.flowia_api.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.domain.job.JobCriteria;
import com.br.rianlucas.flowia_api.domain.job.JobStatus;
import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.domain.user.UserRole;
import com.br.rianlucas.flowia_api.dtos.job.CreateJobRequestDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobPublicResponseDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobResponseDTO;
import com.br.rianlucas.flowia_api.dtos.job.UpdateJobRequestDTO;
import com.br.rianlucas.flowia_api.infra.exceptions.InvalidJobCriteriaException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobNotFoundException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobOwnershipException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobStatusTransitionException;
import com.br.rianlucas.flowia_api.repositories.JobRepository;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobService jobService;

    private User recruiter;
    private Job job;
    private JobCriteria criteria;

    @BeforeEach
    void setUp() {
        recruiter = new User(
                "recrutador@email.com",
                "123456",
                "recrutador",
                "Recrutador Teste",
                UserRole.USER);
        recruiter.setId("recruiter-1");

        criteria = new JobCriteria();
        criteria.setWeights(new JobCriteria.WeightCriteria(30, 30, 20, 10, 10));

        job = new Job();
        job.setId("job-1");
        job.setTitle("Desenvolvedor Java");
        job.setDescription("Vaga para desenvolvedor Java pleno");
        job.setModality("Remoto");
        job.setSalary("R$ 8.000");
        job.setCity("São Paulo");
        job.setState("SP");
        job.setStatus(JobStatus.OPEN);
        job.setCriteria(criteria);
        job.setRecruiter(recruiter);
        job.setCreatedAt(LocalDateTime.now());
    }

    // ==================== CREATE ====================

    @Test
    void deveCriarJobComSucesso() {
        CreateJobRequestDTO dto = new CreateJobRequestDTO(
                "Desenvolvedor Java",
                "Vaga para desenvolvedor Java pleno",
                "Remoto",
                "R$ 8.000",
                "São Paulo",
                "SP",
                criteria);

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job savedJob = invocation.getArgument(0);
            savedJob.setId("job-1");
            return savedJob;
        });

        JobResponseDTO response = jobService.create(dto, recruiter);

        assertNotNull(response);
        assertEquals("Desenvolvedor Java", response.title());
        assertEquals("Remoto", response.modality());
        assertEquals(JobStatus.OPEN, response.status());
        assertEquals("recruiter-1", response.recruiterId());
        verify(jobRepository).save(any(Job.class));
    }

    // ==================== GET BY ID ====================

    @Test
    void deveRetornarJobPorId() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        JobResponseDTO response = jobService.getById("job-1");

        assertNotNull(response);
        assertEquals("job-1", response.id());
        assertEquals("Desenvolvedor Java", response.title());
    }

    @Test
    void deveLancarExcecaoQuandoJobNaoEncontradoPorId() {
        when(jobRepository.findById("job-inexistente")).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getById("job-inexistente"));
    }

    // ==================== GET ALL ====================

    @Test
    void deveRetornarListaDeJobs() {
        when(jobRepository.findAll()).thenReturn(List.of(job));

        List<JobResponseDTO> response = jobService.getAll();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("job-1", response.get(0).id());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaJobs() {
        when(jobRepository.findAll()).thenReturn(Collections.emptyList());

        List<JobResponseDTO> response = jobService.getAll();

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    // ==================== GET BY RECRUITER ====================

    @Test
    void deveRetornarJobsDoRecrutador() {
        when(jobRepository.findByRecruiterId("recruiter-1")).thenReturn(List.of(job));

        List<JobResponseDTO> response = jobService.getByRecruiter(recruiter);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("job-1", response.get(0).id());
    }

    @Test
    void deveRetornarListaVaziaQuandoRecrutadorNaoTemJobs() {
        when(jobRepository.findByRecruiterId("recruiter-1")).thenReturn(Collections.emptyList());

        List<JobResponseDTO> response = jobService.getByRecruiter(recruiter);

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    // ==================== GET JOB PUBLIC ====================

    @Test
    void deveRetornarJobPublico() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        JobPublicResponseDTO response = jobService.getJobPublic("job-1");

        assertNotNull(response);
        assertEquals("job-1", response.id());
        assertEquals("Desenvolvedor Java", response.title());
        assertEquals("Remoto", response.modality());
        assertEquals("R$ 8.000", response.salary());
        assertEquals("São Paulo", response.city());
        assertEquals("SP", response.state());
        assertEquals(JobStatus.OPEN, response.status());
    }

    @Test
    void deveLancarExcecaoQuandoJobPublicoNaoEncontrado() {
        when(jobRepository.findById("job-inexistente")).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getJobPublic("job-inexistente"));
    }

    // ==================== UPDATE - OWNERSHIP ====================

    @Test
    void deveLancarExcecaoQuandoRecrutadorNaoEDonoDoJob() {
        User outroRecruiter = new User(
                "outro@email.com",
                "123456",
                "outro",
                "Outro Recrutador",
                UserRole.USER);
        outroRecruiter.setId("recruiter-2");

        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                "Novo Título", null, null, null, null, null, null, null);

        assertThrows(JobOwnershipException.class, () -> jobService.update("job-1", dto, outroRecruiter));
    }

    // ==================== UPDATE - STATUS TRANSITIONS VALID ====================

    @Test
    void deveTransitarDeDraftParaOpen() {
        job.setStatus(JobStatus.DRAFT);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.OPEN, null);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertNotNull(response);
        assertEquals(JobStatus.OPEN, response.status());
    }

    @Test
    void deveTransitarDeOpenParaClosed() {
        job.setStatus(JobStatus.OPEN);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.CLOSED, null);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertEquals(JobStatus.CLOSED, response.status());
    }

    @Test
    void deveTransitarDeOpenParaPaused() {
        job.setStatus(JobStatus.OPEN);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.PAUSED, null);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertEquals(JobStatus.PAUSED, response.status());
    }

    @Test
    void deveTransitarDePausedParaOpen() {
        job.setStatus(JobStatus.PAUSED);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.OPEN, null);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertEquals(JobStatus.OPEN, response.status());
    }

    @Test
    void deveTransitarDePausedParaClosed() {
        job.setStatus(JobStatus.PAUSED);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.CLOSED, null);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertEquals(JobStatus.CLOSED, response.status());
    }

    // ==================== UPDATE - STATUS TRANSITIONS INVALID ====================

    @Test
    void deveLancarExcecaoAoTransitarDeDraftParaClosed() {
        job.setStatus(JobStatus.DRAFT);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.CLOSED, null);

        assertThrows(JobStatusTransitionException.class,
                () -> jobService.update("job-1", dto, recruiter));
    }

    @Test
    void deveLancarExcecaoAoTransitarDeDraftParaPaused() {
        job.setStatus(JobStatus.DRAFT);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.PAUSED, null);

        assertThrows(JobStatusTransitionException.class,
                () -> jobService.update("job-1", dto, recruiter));
    }

    @Test
    void deveLancarExcecaoAoTransitarDeOpenParaDraft() {
        job.setStatus(JobStatus.OPEN);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.DRAFT, null);

        assertThrows(JobStatusTransitionException.class,
                () -> jobService.update("job-1", dto, recruiter));
    }

    @Test
    void deveLancarExcecaoAoTransitarDeClosedParaOpen() {
        job.setStatus(JobStatus.CLOSED);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.OPEN, null);

        assertThrows(JobStatusTransitionException.class,
                () -> jobService.update("job-1", dto, recruiter));
    }

    @Test
    void deveLancarExcecaoAoTransitarDePausedParaDraft() {
        job.setStatus(JobStatus.PAUSED);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, JobStatus.DRAFT, null);

        assertThrows(JobStatusTransitionException.class,
                () -> jobService.update("job-1", dto, recruiter));
    }

    // ==================== UPDATE - CRITERIA ====================

    @Test
    void deveAtualizarCriteriaComSucesso() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        JobCriteria newCriteria = new JobCriteria();
        newCriteria.setWeights(new JobCriteria.WeightCriteria(25, 25, 25, 15, 10));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, null, newCriteria);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertNotNull(response);
        assertEquals(newCriteria, response.criteria());
        assertNotNull(response.criteriaUpdatedAt());
    }

    @Test
    void deveLancarExcecaoAoAlterarCriteriaDeJobClosed() {
        job.setStatus(JobStatus.CLOSED);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        JobCriteria newCriteria = new JobCriteria();
        newCriteria.setWeights(new JobCriteria.WeightCriteria(25, 25, 25, 15, 10));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, null, newCriteria);

        assertThrows(InvalidJobCriteriaException.class,
                () -> jobService.update("job-1", dto, recruiter));
    }

    @Test
    void deveLancarExcecaoQuandoPesosNaoSomam100() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        JobCriteria invalidCriteria = new JobCriteria();
        invalidCriteria.setWeights(new JobCriteria.WeightCriteria(10, 10, 10, 10, 10));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, null, invalidCriteria);

        assertThrows(InvalidJobCriteriaException.class,
                () -> jobService.update("job-1", dto, recruiter));
    }

    @Test
    void deveAceitarPesosNulos() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        JobCriteria criteriaNullWeights = new JobCriteria();
        criteriaNullWeights.setWeights(null);

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, null, criteriaNullWeights);

        assertDoesNotThrow(() -> jobService.update("job-1", dto, recruiter));
    }

    // ==================== UPDATE - MODALITY ON CLOSED ====================

    @Test
    void deveLancarExcecaoAoAlterarModalidadeDeJobClosed() {
        job.setStatus(JobStatus.CLOSED);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, "Presencial", null, null, null, null, null);

        assertThrows(InvalidJobCriteriaException.class,
                () -> jobService.update("job-1", dto, recruiter));
    }

    // ==================== UPDATE - PARTIAL UPDATE ====================

    @Test
    void deveManterValoresExistentesQuandoCampoENulo() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, null, null);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertEquals("Desenvolvedor Java", response.title());
        assertEquals("Remoto", response.modality());
        assertEquals("R$ 8.000", response.salary());
        assertEquals("São Paulo", response.city());
        assertEquals("SP", response.state());
        assertEquals(JobStatus.OPEN, response.status());
    }

    @Test
    void deveAtualizarApenasCamposFornecidos() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                "Novo Título", null, "Híbrido", null, null, null, null, null);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertEquals("Novo Título", response.title());
        assertEquals("Híbrido", response.modality());
        assertEquals("R$ 8.000", response.salary());
    }

    @Test
    void deveAtualizarCriteriaUpdatedAtQuandoCriteriaMuda() {
        assertNull(job.getCriteriaUpdatedAt());

        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        JobCriteria newCriteria = new JobCriteria();
        newCriteria.setWeights(new JobCriteria.WeightCriteria(20, 30, 20, 15, 15));

        UpdateJobRequestDTO dto = new UpdateJobRequestDTO(
                null, null, null, null, null, null, null, newCriteria);

        JobResponseDTO response = jobService.update("job-1", dto, recruiter);

        assertNotNull(response.criteriaUpdatedAt());
    }
}
