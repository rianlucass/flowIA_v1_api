package com.br.rianlucas.flowia_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.rianlucas.flowia_api.dtos.analysis.CreateResumeAnalysisRequestDTO;
import com.br.rianlucas.flowia_api.dtos.analysis.ResumeAnalysisResponseDTO;
import com.br.rianlucas.flowia_api.services.AnalysisService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/analysis")
public class ResumeAnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<ResumeAnalysisResponseDTO> create(@RequestBody @Valid CreateResumeAnalysisRequestDTO data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(analysisService.create(data));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ResumeAnalysisResponseDTO> getAnalysisByJobId(@PathVariable String jobId) {
        return ResponseEntity.ok(analysisService.getCandidatesAnalysisByJobId(jobId));
    }
                

}