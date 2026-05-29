package com.br.rianlucas.flowia_api.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${uploads.base-dir}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    /**
     * Salva o PDF em uploads/{recruiterId}/{jobId}/{candidateId}.pdf
     * Retorna o caminho relativo para armazenar em resume_url.
     */
    public String storeResume(MultipartFile file, String recruiterId, String jobId, String candidateId) {
        try {
            Path targetDir = baseDir.resolve(recruiterId).resolve(jobId);
            Files.createDirectories(targetDir);

            String filename = candidateId + ".pdf";
            Path targetPath = targetDir.resolve(filename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return recruiterId + "/" + jobId + "/" + filename;

        } catch (IOException ex) {
            throw new RuntimeException("Falha ao armazenar currículo para candidato " + candidateId, ex);
        }
    }

    /**
     * Remove o arquivo de um candidato, se existir.
     */
    public void deleteResume(String recruiterId, String jobId, String candidateId) {
        try {
            Path target = baseDir.resolve(recruiterId).resolve(jobId).resolve(candidateId + ".pdf");
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao remover currículo do candidato " + candidateId, ex);
        }
    }
}
