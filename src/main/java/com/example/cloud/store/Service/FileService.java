package com.example.cloud.store.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFile(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path targetLocation = dir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "File uploaded successfully: " + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + filename, ex);
        }
    }

    public Resource loadFile(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().resolve(filename).normalize();
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + filename);
            }
            return new FileSystemResource(filePath);
        } catch (Exception ex) {
            throw new RuntimeException("Could not load file " + filename, ex);
        }
    }

    public String deleteFile(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            return "File deleted successfully: " + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + filename, ex);
        }
    }
}
