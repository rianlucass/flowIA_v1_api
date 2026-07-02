package com.br.rianlucas.flowia_api.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.br.rianlucas.flowia_api.domain.analysis.AnalysisStatus;
import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.domain.user.UserRole;
import com.br.rianlucas.flowia_api.dtos.analysis.CandidateAnalysisResponseDTO;
import com.br.rianlucas.flowia_api.dtos.analysis.CreateCandidateAnalysisRequestDTO;
import com.br.rianlucas.flowia_api.services.AnalysisService;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class CandidateAnalysisControllerTest {

    @Mock
    private AnalysisService analysisService;

    @InjectMocks
    private CandidateAnalysisController controller;

    @Test
    void deveCriarAnaliseComSucesso() {
        CreateCandidateAnalysisRequestDTO dto = new CreateCandidateAnalysisRequestDTO(
                "candidate-1", "job-1",
                null, null, null, null, null, null, null,
                new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("80"),
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                AnalysisStatus.COMPLETED,
                null, null, null, null, null, null, null, null, null, null, null);

        CandidateAnalysisResponseDTO responseDTO = new CandidateAnalysisResponseDTO(
                "analysis-1", "candidate-1", "Nome", "email@teste.com", "11999999999",
                "job-1",
                new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("80"),
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                AnalysisStatus.COMPLETED,
                null, null, null, null, null, null, null, null, null, null, null, null);

        when(analysisService.create(dto)).thenReturn(responseDTO);

        ResponseEntity<CandidateAnalysisResponseDTO> response = controller.create(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("analysis-1", response.getBody().id());

        verify(analysisService).create(dto);
    }

    @Test
    void deveListarAnalisesPorJobId() {
        User user = new User("email@email.com", "123", "user", "Nome", UserRole.USER);
        user.setId("recruiter-1");

        CandidateAnalysisResponseDTO analysis = new CandidateAnalysisResponseDTO(
                "analysis-1", "candidate-1", "Nome", "email@teste.com", "11999999999",
                "job-1",
                new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("80"),
                new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("80"),
                AnalysisStatus.COMPLETED,
                null, null, null, null, null, null, null, null, null, null, null, null);

        when(analysisService.getAllAnalysisByJobId("job-1", user)).thenReturn(List.of(analysis));

        ResponseEntity<List<CandidateAnalysisResponseDTO>> response = controller.getAllAnalysisByJobId("job-1", user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(analysisService).getAllAnalysisByJobId("job-1", user);
    }
}
