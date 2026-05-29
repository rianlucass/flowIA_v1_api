package com.br.rianlucas.flowia_api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.br.rianlucas.flowia_api.dtos.candidate.CandidateResponseDTO;
import com.br.rianlucas.flowia_api.dtos.candidate.CandidateUploadResponseDTO;
import com.br.rianlucas.flowia_api.dtos.candidate.CreateCandidateRequestDTO;
import com.br.rianlucas.flowia_api.services.CandidatesService;
import com.br.rianlucas.flowia_api.services.OcrService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/candidates")
public class CandidateController {

    @Autowired
    private CandidatesService candidatesService;

    @Autowired
    private OcrService ocrService;

    @PostMapping
    public ResponseEntity<CandidateResponseDTO> create(@RequestBody @Valid CreateCandidateRequestDTO data) {
        CandidateResponseDTO candidate = candidatesService.create(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<CandidateResponseDTO>> getCandidatesByJobId(@PathVariable String jobId) {
        return ResponseEntity.ok(candidatesService.getCandidatesByJobId(jobId));
    }

    @PostMapping("/upload")
    public ResponseEntity<CandidateUploadResponseDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobId") String jobId,
            @RequestParam("name") String name,
            @RequestParam("email") String email) {

        String resumeText = ocrService.extractText(file);
        CandidateResponseDTO candidate = candidatesService.createFromUpload(jobId, name, email, resumeText);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CandidateUploadResponseDTO(
                candidate.id(),
                candidate.jobId(),
                candidate.name(),
                candidate.resumeText(),
                candidate.processedByAi()
        ));
    }
}
