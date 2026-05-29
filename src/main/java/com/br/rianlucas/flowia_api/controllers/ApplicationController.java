package com.br.rianlucas.flowia_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.br.rianlucas.flowia_api.dtos.candidate.ApplyJobResponseDTO;
import com.br.rianlucas.flowia_api.services.CandidatesService;

@RestController
@RequestMapping("/jobs")
public class ApplicationController {

    @Autowired
    private CandidatesService candidatesService;

    /**
     * Endpoint público — acessado pelo candidato via frontend Next.js.
     * Não requer autenticação JWT.
     */
    @PostMapping("/{jobId}/apply")
    public ResponseEntity<ApplyJobResponseDTO> apply(
            @PathVariable String jobId,
            @RequestParam("file") MultipartFile file) {

        ApplyJobResponseDTO response = candidatesService.apply(jobId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
