package com.br.rianlucas.flowia_api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.rianlucas.flowia_api.dtos.candidate.CandidateResponseDTO;
import com.br.rianlucas.flowia_api.services.CandidatesService;

@RestController
@RequestMapping("/candidates")
public class CandidateController {

    @Autowired
    private CandidatesService candidatesService;

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<CandidateResponseDTO>> getCandidatesByJobId(@PathVariable String jobId) {
        return ResponseEntity.ok(candidatesService.getCandidatesByJobId(jobId));
    }
}
