package com.br.rianlucas.flowia_api.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.br.rianlucas.flowia_api.domain.analysis.AnalysisStatus;
import com.br.rianlucas.flowia_api.domain.analysis.CandidateAnalysis;
import com.br.rianlucas.flowia_api.domain.candidates.Candidate;
import com.br.rianlucas.flowia_api.domain.candidates.CandidateStatus;
import com.br.rianlucas.flowia_api.domain.job.Job;
import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.domain.user.UserRole;
import com.br.rianlucas.flowia_api.dtos.analysis.CandidateAnalysisResponseDTO;
import com.br.rianlucas.flowia_api.dtos.analysis.CreateCandidateAnalysisRequestDTO;
import com.br.rianlucas.flowia_api.infra.exceptions.CandidateNotFoundException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobNotFoundException;
import com.br.rianlucas.flowia_api.infra.exceptions.JobOwnershipException;
import com.br.rianlucas.flowia_api.repositories.CandidateAnalysisRepository;
import com.br.rianlucas.flowia_api.repositories.CandidateRepository;
import com.br.rianlucas.flowia_api.repositories.JobRepository;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private CandidateAnalysisRepository candidateAnalysisRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private Candidate candidate;
    private Job job;
    private CandidateAnalysis analysis;
    private CreateCandidateAnalysisRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setName("Candidato Teste");
        candidate.setEmail("candidato@email.com");
        candidate.setStatus(CandidateStatus.RECEIVED);
        candidate.setProcessedByAi(false);

        job = new Job();
        job.setId("job-1");

        analysis = new CandidateAnalysis();
        analysis.setId("analysis-1");
        analysis.setCandidate(candidate);
        analysis.setJob(job);
        analysis.setFinalScore(new BigDecimal("85.5"));
        analysis.setStatus(AnalysisStatus.COMPLETED);

        requestDTO = new CreateCandidateAnalysisRequestDTO(
                "candidate-1", "job-1",
                "Candidato IA", "ia@email.com", "11988887777",
                "Rio de Janeiro", "RJ",
                "https://linkedin.com/in/ia", "https://portfolio.com/ia",
                new BigDecimal("85.5"), new BigDecimal("90"), new BigDecimal("80"),
                new BigDecimal("75"), new BigDecimal("85"), new BigDecimal("70"),
                AnalysisStatus.APPROVED,
                Map.of("lideranca", "forte"), Map.of("ingles", "basico"),
                Map.of("certificacao", "nao informada"), Map.of("pergunta1", "Qual sua experiencia?"),
                "Recomendado para entrevista", Map.of("scoreMinimo", "atingido"),
                Map.of("atividades", 30), List.of("Faltou certificacao obrigatoria"),
                "GPT-4", "v2", false);
    }

    @Test
    void deveCriarAnaliseComSucesso() {
        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateAnalysisRepository.save(any(CandidateAnalysis.class))).thenReturn(analysis);

        CandidateAnalysisResponseDTO response = analysisService.create(requestDTO);

        assertNotNull(response);
        assertEquals("analysis-1", response.id());
        assertEquals("candidate-1", response.candidateId());
        assertEquals(new BigDecimal("85.5"), response.finalScore());

        verify(candidateRepository).findById("candidate-1");
        verify(jobRepository).findById("job-1");
        verify(candidateRepository).save(candidate);
        verify(candidateAnalysisRepository).save(any(CandidateAnalysis.class));
    }

    @Test
    void deveEnriquecerCandidatoComDadosDoAI() {
        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateAnalysisRepository.save(any(CandidateAnalysis.class))).thenReturn(analysis);

        analysisService.create(requestDTO);

        assertEquals("Candidato IA", candidate.getName());
        assertEquals("ia@email.com", candidate.getEmail());
        assertEquals("11988887777", candidate.getPhone());
        assertEquals("Rio de Janeiro", candidate.getCity());
        assertEquals("RJ", candidate.getState());
        assertTrue(candidate.getProcessedByAi());
    }

    @Test
    void deveSincronizarStatusCandidatoComoAprovado() {
        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateAnalysisRepository.save(any(CandidateAnalysis.class))).thenReturn(analysis);

        analysisService.create(requestDTO);

        assertEquals(CandidateStatus.APPROVED, candidate.getStatus());
    }

    @Test
    void deveSincronizarStatusCandidatoComoRejeitado() {
        createAndAssertStatusSync(AnalysisStatus.REJECTED, CandidateStatus.REJECTED);
    }

    @Test
    void deveSincronizarStatusCandidatoComoRevisao() {
        createAndAssertStatusSync(AnalysisStatus.REVIEW, CandidateStatus.REVIEW);
    }

    private void createAndAssertStatusSync(AnalysisStatus analysisStatus, CandidateStatus expectedStatus) {
        CreateCandidateAnalysisRequestDTO dto = new CreateCandidateAnalysisRequestDTO(
                "candidate-1", "job-1",
                null, null, null, null, null, null, null,
                new BigDecimal("60"), new BigDecimal("50"), new BigDecimal("60"),
                new BigDecimal("70"), new BigDecimal("55"), new BigDecimal("65"),
                analysisStatus,
                null, null, null, null, null, null, null, null, null, null, null);

        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateAnalysisRepository.save(any(CandidateAnalysis.class))).thenReturn(analysis);

        analysisService.create(dto);

        assertEquals(expectedStatus, candidate.getStatus());
    }

    @Test
    void deveIgnorarPlaceholderInformacaoNaoEncontrada() {
        CreateCandidateAnalysisRequestDTO dto = new CreateCandidateAnalysisRequestDTO(
                "candidate-1", "job-1",
                "informação não encontrada", "informação não encontrada",
                null, "informação não encontrada", null, null, null,
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                AnalysisStatus.COMPLETED,
                null, null, null, null, null, null, null, null, null, null, null);

        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateAnalysisRepository.save(any(CandidateAnalysis.class))).thenReturn(analysis);

        analysisService.create(dto);

        assertEquals("Candidato Teste", candidate.getName());
        assertEquals("candidato@email.com", candidate.getEmail());
    }

    @Test
    void deveSetarOutdatedCorretamente() {
        CreateCandidateAnalysisRequestDTO dto = new CreateCandidateAnalysisRequestDTO(
                "candidate-1", "job-1",
                null, null, null, null, null, null, null,
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                AnalysisStatus.COMPLETED,
                null, null, null, null, null, null, null, null, "v1", "v1", true);

        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateAnalysisRepository.save(any(CandidateAnalysis.class))).thenAnswer(inv -> {
            CandidateAnalysis a = inv.getArgument(0);
            a.setId("analysis-1");
            a.setCandidate(candidate);
            a.setJob(job);
            return a;
        });

        CandidateAnalysisResponseDTO response = analysisService.create(dto);

        assertTrue(response.outdated());
    }

    @Test
    void deveLancarExcecaoQuandoCandidatoNaoEncontrado() {
        when(candidateRepository.findById("candidate-inexistente")).thenReturn(Optional.empty());

        CreateCandidateAnalysisRequestDTO dto = new CreateCandidateAnalysisRequestDTO(
                "candidate-inexistente", "job-1",
                null, null, null, null, null, null, null,
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                AnalysisStatus.COMPLETED,
                null, null, null, null, null, null, null, null, null, null, null);

        assertThrows(CandidateNotFoundException.class, () -> analysisService.create(dto));
    }

    @Test
    void deveLancarExcecaoQuandoJobNaoEncontrado() {
        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById("job-inexistente")).thenReturn(Optional.empty());

        CreateCandidateAnalysisRequestDTO dto = new CreateCandidateAnalysisRequestDTO(
                "candidate-1", "job-inexistente",
                null, null, null, null, null, null, null,
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                AnalysisStatus.COMPLETED,
                null, null, null, null, null, null, null, null, null, null, null);

        assertThrows(JobNotFoundException.class, () -> analysisService.create(dto));
    }

    @Test
    void deveBuscarAnalisePorJobId() {
        when(candidateAnalysisRepository.findFirstByJobId("job-1")).thenReturn(Optional.of(analysis));

        CandidateAnalysisResponseDTO response = analysisService.getAnalysisByJobId("job-1");

        assertNotNull(response);
        assertEquals("analysis-1", response.id());
        verify(candidateAnalysisRepository).findFirstByJobId("job-1");
    }

    @Test
    void deveLancarExcecaoQuandoAnaliseNaoEncontradaPorJobId() {
        when(candidateAnalysisRepository.findFirstByJobId("job-inexistente")).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> analysisService.getAnalysisByJobId("job-inexistente"));
    }

    @Test
    void deveListarTodasAnalisesPorJobId() {
        User recruiter = new User("email@email.com", "123", "user", "Nome", UserRole.USER);
        recruiter.setId("recruiter-1");
        job.setRecruiter(recruiter);

        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(candidateAnalysisRepository.findByJobId("job-1")).thenReturn(List.of(analysis));

        List<CandidateAnalysisResponseDTO> response = analysisService.getAllAnalysisByJobId("job-1", recruiter);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("analysis-1", response.get(0).id());
    }

    @Test
    void deveLancarExcecaoQuandoNaoForDonoDaVaga() {
        User recruiter = new User("email@email.com", "123", "user", "Nome", UserRole.USER);
        recruiter.setId("recruiter-1");
        User outroRecruiter = new User("outro@email.com", "123", "outro", "Outro", UserRole.USER);
        outroRecruiter.setId("recruiter-2");
        job.setRecruiter(recruiter);

        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        assertThrows(JobOwnershipException.class,
                () -> analysisService.getAllAnalysisByJobId("job-1", outroRecruiter));
    }

    @Test
    void deveLancarExcecaoQuandoJobNaoExisteNaListagem() {
        User recruiter = new User("email@email.com", "123", "user", "Nome", UserRole.USER);
        recruiter.setId("recruiter-1");

        when(jobRepository.findById("job-inexistente")).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class,
                () -> analysisService.getAllAnalysisByJobId("job-inexistente", recruiter));
    }
}
