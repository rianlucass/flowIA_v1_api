package com.br.rianlucas.flowia_api.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void deveSalvarResumeComSucesso() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "curriculo.pdf", "application/pdf", "conteudo".getBytes());

        String result = fileStorageService.storeResume(file, "recruiter-1", "job-1", "candidate-1");

        assertEquals("recruiter-1/job-1/candidate-1.pdf", result);

        Path expectedPath = tempDir.resolve("recruiter-1").resolve("job-1").resolve("candidate-1.pdf");
        assertTrue(Files.exists(expectedPath));
    }

    @Test
    void deveCriarDiretoriosSeNaoExistirem() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "curriculo.pdf", "application/pdf", "dados".getBytes());

        Path targetDir = tempDir.resolve("recruiter-2").resolve("job-2");
        assertFalse(Files.exists(targetDir));

        fileStorageService.storeResume(file, "recruiter-2", "job-2", "candidate-2");

        assertTrue(Files.exists(targetDir));
    }

    @Test
    void deveSobrescreverArquivoExistente() {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "curriculo.pdf", "application/pdf", "v1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "curriculo.pdf", "application/pdf", "v2".getBytes());

        fileStorageService.storeResume(file1, "r1", "j1", "c1");
        fileStorageService.storeResume(file2, "r1", "j1", "c1");

        Path path = tempDir.resolve("r1").resolve("j1").resolve("c1.pdf");
        assertTrue(Files.exists(path));
    }

    @Test
    void deveLancarExcecaoQuandoFalhaAoSalvar() throws IOException {
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("Erro de IO"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileStorageService.storeResume(file, "r1", "j1", "c1"));

        assertTrue(exception.getMessage().contains("Falha ao armazenar"));
    }

    @Test
    void deveDeletarResumeComSucesso() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "curriculo.pdf", "application/pdf", "conteudo".getBytes());

        fileStorageService.storeResume(file, "r1", "j1", "c1");
        Path expectedPath = tempDir.resolve("r1").resolve("j1").resolve("c1.pdf");
        assertTrue(Files.exists(expectedPath));

        fileStorageService.deleteResume("r1", "j1", "c1");

        assertFalse(Files.exists(expectedPath));
    }

    @Test
    void deveIgnorarDelecaoDeArquivoInexistente() {
        assertDoesNotThrow(() -> fileStorageService.deleteResume("r1", "j1", "c99"));
    }
}
