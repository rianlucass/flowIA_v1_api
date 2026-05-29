package com.br.rianlucas.flowia_api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.rianlucas.flowia_api.domain.user.User;

import com.br.rianlucas.flowia_api.dtos.analysis.CandidateAnalysisResponseDTO;
import com.br.rianlucas.flowia_api.dtos.analysis.CreateCandidateAnalysisRequestDTO;
import com.br.rianlucas.flowia_api.services.AnalysisService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/analysis")
public class CandidateAnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<CandidateAnalysisResponseDTO> create(@RequestBody @Valid CreateCandidateAnalysisRequestDTO data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(analysisService.create(data));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<CandidateAnalysisResponseDTO>> getAllAnalysisByJobId(
            @PathVariable String jobId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analysisService.getAllAnalysisByJobId(jobId, user));
    }
}