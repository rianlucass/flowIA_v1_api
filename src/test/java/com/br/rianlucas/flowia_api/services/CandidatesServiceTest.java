package com.br.rianlucas.flowia_api.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.br.rianlucas.flowia_api.domain.candidates.Candidate;
import com.br.rianlucas.flowia_api.domain.candidates.CandidateStatus;
import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.domain.job.JobCriteria;
import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.domain.user.UserRole;
import com.br.rianlucas.flowia_api.dtos.candidate.ApplyJobResponseDTO;
import com.br.rianlucas.flowia_api.dtos.candidate.CandidateResponseDTO;
import com.br.rianlucas.flowia_api.dtos.candidate.CreateCandidateRequestDTO;
import com.br.rianlucas.flowia_api.infra.exceptions.JobNotFoundException;
import com.br.rianlucas.flowia_api.infra.exceptions.OcrServiceException;
import com.br.rianlucas.flowia_api.repositories.CandidateRepository;
import com.br.rianlucas.flowia_api.repositories.JobRepository;

@ExtendWith(MockitoExtension.class)
class CandidatesServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private OcrService ocrService;

    @Mock
    private N8NWebhookService n8nWebhookService;

    @InjectMocks
    private CandidatesService candidatesService;

    private Job job;
    private Candidate candidate;
    private MockMultipartFile pdfFile;
    private MockMultipartFile nonPdfFile;

    @BeforeEach
    void setUp() {
        User recruiter = new User(
                "recrutador@email.com",
                "123456",
                "recrutador",
                "Recrutador Teste",
                UserRole.USER);
        recruiter.setId("recruiter-1");

        job = new Job();
        job.setId("job-1");
        job.setTitle("Desenvolvedor Java");
        job.setRecruiter(recruiter);
        job.setCriteria(new JobCriteria());

        candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setJob(job);
        candidate.setName("Candidato Teste");
        candidate.setEmail("candidato@email.com");
        candidate.setStatus(CandidateStatus.RECEIVED);
        candidate.setProcessedByAi(false);

        pdfFile = new MockMultipartFile(
                "file",
                "curriculo.pdf",
                "application/pdf",
                "conteudo do pdf".getBytes());

        nonPdfFile = new MockMultipartFile(
                "file",
                "foto.png",
                "image/png",
                "imagem".getBytes());
    }

    // ==================== GET CANDIDATES BY JOB ID ====================

    @Test
    void deveRetornarCandidatosPorJobId() {
        when(candidateRepository.findByJobId("job-1")).thenReturn(List.of(candidate));

        List<CandidateResponseDTO> response = candidatesService.getCandidatesByJobId("job-1");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("candidate-1", response.get(0).id());
        assertEquals("Candidato Teste", response.get(0).name());
        assertEquals("job-1", response.get(0).jobId());
    }

    @Test
    void deveRetornarListaVaziaQuandoJobNaoTemCandidatos() {
        when(candidateRepository.findByJobId("job-1")).thenReturn(Collections.emptyList());

        List<CandidateResponseDTO> response = candidatesService.getCandidatesByJobId("job-1");

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    // ==================== APPLY ====================

    @Test
    void deveAplicarComSucesso() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            if (c.getId() == null) c.setId("candidate-1");
            return c;
        });
        when(fileStorageService.storeResume(any(), eq("recruiter-1"), eq("job-1"), eq("candidate-1")))
                .thenReturn("recruiter-1/job-1/candidate-1.pdf");
        when(ocrService.extractText(pdfFile)).thenReturn("Texto extraído do currículo");
        doNothing().when(n8nWebhookService).notifyApplication(any());

        ApplyJobResponseDTO response = candidatesService.apply("job-1", pdfFile);

        assertNotNull(response);
        assertEquals("candidate-1", response.candidateId());
        assertEquals("job-1", response.jobId());
        assertEquals("recruiter-1/job-1/candidate-1.pdf", response.resumeUrl());
        assertEquals(CandidateStatus.PROCESSING, response.status());

        verify(jobRepository).findById("job-1");
        verify(candidateRepository, times(3)).save(any(Candidate.class));
        verify(fileStorageService).storeResume(any(), eq("recruiter-1"), eq("job-1"), eq("candidate-1"));
        verify(ocrService).extractText(pdfFile);
        verify(n8nWebhookService).notifyApplication(any());
    }

    @Test
    void deveLancarExcecaoQuandoArquivoNaoEPdf() {
        assertThrows(IllegalArgumentException.class,
                () -> candidatesService.apply("job-1", nonPdfFile));
    }

    @Test
    void deveLancarExcecaoQuandoJobNaoEncontradoNoApply() {
        when(jobRepository.findById("job-inexistente")).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class,
                () -> candidatesService.apply("job-inexistente", pdfFile));
    }

    @Test
    void deveContinuarProcessamentoMesmoComFalhaNoOcr() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            if (c.getId() == null) c.setId("candidate-1");
            return c;
        });
        when(fileStorageService.storeResume(any(), eq("recruiter-1"), eq("job-1"), eq("candidate-1")))
                .thenReturn("recruiter-1/job-1/candidate-1.pdf");
        when(ocrService.extractText(pdfFile)).thenThrow(new OcrServiceException("OCR service unavailable"));
        doNothing().when(n8nWebhookService).notifyApplication(any());

        ApplyJobResponseDTO response = candidatesService.apply("job-1", pdfFile);

        assertNotNull(response);
        assertEquals("candidate-1", response.candidateId());

        verify(n8nWebhookService).notifyApplication(any());
    }

    // ==================== CREATE ====================

    @Test
    void deveCriarCandidatoComSucesso() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            if (c.getId() == null) c.setId("candidate-1");
            return c;
        });

        CreateCandidateRequestDTO dto = new CreateCandidateRequestDTO(
                "job-1",
                "Candidato Teste",
                "candidato@email.com",
                "11999999999",
                "São Paulo",
                "SP",
                "https://linkedin.com/in/candidato",
                "https://portfolio.com/candidato",
                "http://resumes/candidato.pdf",
                "resume text content",
                CandidateStatus.REVIEW,
                true);

        CandidateResponseDTO response = candidatesService.create(dto);

        assertNotNull(response);
        assertEquals("candidate-1", response.id());
        assertEquals("job-1", response.jobId());
        assertEquals("Candidato Teste", response.name());
        assertEquals(CandidateStatus.REVIEW, response.status());
        assertTrue(response.processedByAi());
    }

    @Test
    void deveCriarCandidatoComStatusPadraoQuandoNaoInformado() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            if (c.getId() == null) c.setId("candidate-1");
            return c;
        });

        CreateCandidateRequestDTO dto = new CreateCandidateRequestDTO(
                "job-1",
                "Candidato Teste",
                "candidato@email.com",
                null, null, null, null, null, null, null,
                null, null);

        CandidateResponseDTO response = candidatesService.create(dto);

        assertNotNull(response);
        assertEquals(CandidateStatus.RECEIVED, response.status());
        assertFalse(response.processedByAi());
    }

    @Test
    void deveCriarCandidatoComProcessedByAiPadraoQuandoNaoInformado() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            if (c.getId() == null) c.setId("candidate-1");
            return c;
        });

        CreateCandidateRequestDTO dto = new CreateCandidateRequestDTO(
                "job-1",
                "Candidato Teste",
                "candidato@email.com",
                null, null, null, null, null, null, null,
                CandidateStatus.REVIEW,
                null);

        CandidateResponseDTO response = candidatesService.create(dto);

        assertNotNull(response);
        assertFalse(response.processedByAi());
    }

    @Test
    void deveLancarExcecaoQuandoJobNaoEncontradoNoCreate() {
        when(jobRepository.findById("job-inexistente")).thenReturn(Optional.empty());

        CreateCandidateRequestDTO dto = new CreateCandidateRequestDTO(
                "job-inexistente",
                "Candidato Teste",
                "candidato@email.com",
                null, null, null, null, null, null, null,
                null, null);

        assertThrows(JobNotFoundException.class, () -> candidatesService.create(dto));
    }

    // ==================== CREATE FROM UPLOAD ====================

    @Test
    void deveCriarCandidatoViaUploadComSucesso() {
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            if (c.getId() == null) c.setId("candidate-1");
            return c;
        });

        CandidateResponseDTO response = candidatesService.createFromUpload(
                "job-1", "Candidato Upload", "upload@email.com", "Texto extraído do PDF");

        assertNotNull(response);
        assertEquals("candidate-1", response.id());
        assertEquals("job-1", response.jobId());
        assertEquals("Candidato Upload", response.name());
        assertEquals("upload@email.com", response.email());
        assertEquals(CandidateStatus.RECEIVED, response.status());
        assertFalse(response.processedByAi());
    }

    @Test
    void deveLancarExcecaoQuandoJobNaoEncontradoNoUpload() {
        when(jobRepository.findById("job-inexistente")).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class,
                () -> candidatesService.createFromUpload(
                        "job-inexistente", "Candidato", "email@email.com", "texto"));
    }
}
