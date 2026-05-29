package com.br.rianlucas.flowia_api.domain.candidates;


import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.br.rianlucas.flowia_api.domain.job.Job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "candidates")
@Getter
@Setter
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String email;

    @Column(columnDefinition = "TEXT")
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String city;

    @Column(columnDefinition = "TEXT")
    private String state;

    @Column(columnDefinition = "TEXT")
    private String linkedinUrl;

    @Column(columnDefinition = "TEXT")
    private String portfolioUrl;

    @Column(columnDefinition = "TEXT")
    private String resumeUrl;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Enumerated(EnumType.STRING)
    private CandidateStatus status;

    @Column(nullable = false)
    private Boolean processedByAi = false;

    @Column(nullable = false)
    private Boolean analysisOutdated = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}