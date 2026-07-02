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
import org.springframework.mock.web.MockMultipartFile;

import com.br.rianlucas.flowia_api.domain.candidates.CandidateStatus;
import com.br.rianlucas.flowia_api.dtos.candidate.CandidateResponseDTO;
import com.br.rianlucas.flowia_api.dtos.candidate.CandidateUploadResponseDTO;
import com.br.rianlucas.flowia_api.dtos.candidate.CreateCandidateRequestDTO;
import com.br.rianlucas.flowia_api.services.CandidatesService;
import com.br.rianlucas.flowia_api.services.OcrService;

@ExtendWith(MockitoExtension.class)
class CandidateControllerTest {

    @Mock
    private CandidatesService candidatesService;

    @Mock
    private OcrService ocrService;

    @InjectMocks
    private CandidateController candidateController;

    private CandidateResponseDTO candidateResponse;
    private MockMultipartFile pdfFile;

    @BeforeEach
    void setUp() {
        candidateResponse = new CandidateResponseDTO(
                "candidate-1",
                "job-1",
                "Candidato Teste",
                "candidato@email.com",
                "11999999999",
                "Sao Paulo",
                "SP",
                "linkedin.com/in/candidato",
                "portfolio.com/candidato",
                "resumes/curriculo.pdf",
                "Texto do curriculo",
                CandidateStatus.RECEIVED,
                false,
                false,
                LocalDateTime.now());

        pdfFile = new MockMultipartFile(
                "file",
                "curriculo.pdf",
                "application/pdf",
                "conteudo do pdf".getBytes());
    }

    @Test
    void deveCriarCandidatoComSucesso() {
        CreateCandidateRequestDTO dto = new CreateCandidateRequestDTO(
                "job-1", "Candidato Teste", "candidato@email.com",
                null, null, null, null, null, null, null, null, null);
        when(candidatesService.create(dto)).thenReturn(candidateResponse);

        ResponseEntity<CandidateResponseDTO> response = candidateController.create(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("candidate-1", response.getBody().id());
        assertEquals("Candidato Teste", response.getBody().name());

        verify(candidatesService).create(dto);
    }

    @Test
    void deveListarCandidatosPorJobId() {
        when(candidatesService.getCandidatesByJobId("job-1")).thenReturn(List.of(candidateResponse));

        ResponseEntity<List<CandidateResponseDTO>> response = candidateController.getCandidatesByJobId("job-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("candidate-1", response.getBody().get(0).id());

        verify(candidatesService).getCandidatesByJobId("job-1");
    }

    @Test
    void deveRetornarListaVazia() {
        when(candidatesService.getCandidatesByJobId("job-1")).thenReturn(List.of());

        ResponseEntity<List<CandidateResponseDTO>> response = candidateController.getCandidatesByJobId("job-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void deveRealizarUploadComSucesso() {
        when(ocrService.extractText(pdfFile)).thenReturn("Texto extraido do curriculo");
        when(candidatesService.createFromUpload("job-1", "Candidato Upload", "upload@email.com", "Texto extraido do curriculo"))
                .thenReturn(candidateResponse);

        ResponseEntity<CandidateUploadResponseDTO> response = candidateController.upload(
                pdfFile, "job-1", "Candidato Upload", "upload@email.com");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("candidate-1", response.getBody().candidateId());
        assertEquals("job-1", response.getBody().jobId());
        assertEquals("Candidato Teste", response.getBody().candidateName());
        assertEquals("Texto do curriculo", response.getBody().resumeText());

        verify(ocrService).extractText(pdfFile);
        verify(candidatesService).createFromUpload("job-1", "Candidato Upload", "upload@email.com", "Texto extraido do curriculo");
    }
}
