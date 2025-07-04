package com.example.cloud.store.Controller;

import com.example.cloud.store.utils.ZipUtility;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String UPLOAD_DIR = "uploads";
    private static final String COMPRESS_DIR = "compressed";
    private static final String EXTRACT_DIR = "extracted";
    private final Set<String> uploadedFiles = new HashSet<>();

    public FileController() throws IOException {
        Files.createDirectories(Path.of(UPLOAD_DIR));
        Files.createDirectories(Path.of(COMPRESS_DIR));
        Files.createDirectories(Path.of(EXTRACT_DIR));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("username") String username) {
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        long fileSize = file.getSize();
        if (user.getUsedStorage() + fileSize > user.getMaxStorage()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Storage quota exceeded (1GB).");
        }

        Path userDir = Paths.get("storage", user.getId().toString());
        try {
            Files.createDirectories(userDir);
            String filename = file.getOriginalFilename();
            Path filePath = userDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            user.setUsedStorage(user.getUsedStorage() + fileSize);
            userRepository.save(user);

            return ResponseEntity.ok("File uploaded.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed.");
        }
    }


    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String filename) {
        if (!uploadedFiles.contains(filename)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("File download blocked. File not uploaded yet.");
        }

        Path filePath = Path.of(UPLOAD_DIR, filename);
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found on server.");
        }

        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(fileBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Download failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        if (!uploadedFiles.contains(filename)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("File delete blocked. File not uploaded yet.");
        }

        Path filePath = Path.of(UPLOAD_DIR, filename);
        try {
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
            }

            Files.delete(filePath);
            uploadedFiles.remove(filename);
            return ResponseEntity.ok("File deleted: " + filename);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Delete failed: " + e.getMessage());
        }
    }

    @PostMapping("/compress/{filename}")
    public ResponseEntity<?> compressFile(@PathVariable String filename) {
        if (!uploadedFiles.contains(filename)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Compression blocked. File not uploaded yet.");
        }

        try {
            Path source = Path.of(UPLOAD_DIR, filename);
            Path zipPath = Path.of(COMPRESS_DIR, filename + ".zip");
            ZipUtility.zip(source, zipPath);
            return ResponseEntity.ok("File compressed: " + zipPath.getFileName());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Compression failed: " + e.getMessage());
        }
    }

    @PostMapping("/extract/{zipname}")
    public ResponseEntity<?> extractZip(@PathVariable String zipname) {
        if (!uploadedFiles.contains(zipname)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Extraction blocked. ZIP file not uploaded yet.");
        }

        try {
            Path zipPath = Path.of(UPLOAD_DIR, zipname);
            Path outputDir = Path.of(EXTRACT_DIR, zipname.replace(".zip", ""));
            Files.createDirectories(outputDir);
            ZipUtility.unzip(zipPath, outputDir);
            return ResponseEntity.ok("ZIP extracted to: " + outputDir.getFileName());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Extraction failed: " + e.getMessage());
        }
    }
}
