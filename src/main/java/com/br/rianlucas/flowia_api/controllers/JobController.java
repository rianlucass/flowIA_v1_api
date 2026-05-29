package com.br.rianlucas.flowia_api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.dtos.job.CreateJobRequestDTO;
import com.br.rianlucas.flowia_api.dtos.job.JobResponseDTO;
import com.br.rianlucas.flowia_api.dtos.job.UpdateJobRequestDTO;
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

    @GetMapping
    public ResponseEntity<List<JobResponseDTO>> getMyJobs(@AuthenticationPrincipal User recruiter) {
        return ResponseEntity.ok(jobService.getByRecruiter(recruiter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> getById(@PathVariable String id) {
        JobResponseDTO response = jobService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JobResponseDTO> update(
            @PathVariable String id,
            @RequestBody UpdateJobRequestDTO data,
            @AuthenticationPrincipal User recruiter) {
        JobResponseDTO response = jobService.update(id, data, recruiter);
        return ResponseEntity.ok(response);
    }
}
