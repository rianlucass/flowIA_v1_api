package com.br.rianlucas.flowia_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.dtos.job.CreateJobRequestDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobResponseDTO;
import com.br.rianlucas.flowia_api.services.JobService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponseDTO> create(
            @RequestBody @Valid CreateJobRequestDTO data,
            @AuthenticationPrincipal User recruiter) {
        JobResponseDTO response = jobService.create(data, recruiter);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
