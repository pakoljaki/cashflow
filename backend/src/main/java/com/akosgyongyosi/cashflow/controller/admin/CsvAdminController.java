package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.service.AuditLogService;
import com.akosgyongyosi.cashflow.service.CsvImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/csv")
@PreAuthorize("hasRole('ADMIN')")
public class CsvAdminController {

    private final CsvImportService csvImportService;
    private final AuditLogService auditLogService;

    @Value("${app.csv.import-dir:csv_imports}")
    private String importDir;

    public CsvAdminController(CsvImportService csvImportService, AuditLogService auditLogService) {
        this.csvImportService = csvImportService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsvFile(@RequestParam("file") MultipartFile file, Principal principal) {
        if (file.isEmpty()) {
            auditLogService.logFailedAction(actor(principal), "CSV_UPLOAD", "EMPTY_FILE");
            return ResponseEntity.badRequest().body("No file uploaded");
        }
        try (InputStream inputStream = file.getInputStream()) {
            csvImportService.parseSingleFile(inputStream, file.getOriginalFilename());
            auditLogService.logAction(actor(principal), "CSV_UPLOAD", 
                Map.of("fileName", file.getOriginalFilename(), 
                       "fileSize", file.getSize()));
            return ResponseEntity.ok("File parsed successfully in memory.");
        } catch (Exception e) {
            auditLogService.logFailedAction(actor(principal), "CSV_UPLOAD", 
                "ERROR: " + e.getMessage());
            return ResponseEntity.status(500).body("Error parsing file: " + e.getMessage());
        }
    }

    @GetMapping("/imports")
    public ResponseEntity<List<String>> listImports() {
        try {
            Path dir = Paths.get(importDir);
            if (!Files.isDirectory(dir)) {
                return ResponseEntity.ok(List.of());
            }
            List<String> files = Files.list(dir)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importExisting(@RequestParam("file") String fileName, Principal principal) {
        Path filePath = Paths.get(importDir, fileName);
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            auditLogService.logFailedAction(actor(principal), "CSV_IMPORT", "FILE_NOT_FOUND: " + fileName);
            return ResponseEntity.badRequest().body("File not found: " + fileName);
        }
        try (InputStream in = Files.newInputStream(filePath)) {
            csvImportService.parseSingleFile(in, fileName);
            auditLogService.logAction(actor(principal), "CSV_IMPORT", 
                Map.of("fileName", fileName));
            return ResponseEntity.ok("Imported " + fileName);
        } catch (Exception e) {
            auditLogService.logFailedAction(actor(principal), "CSV_IMPORT", 
                "ERROR: " + e.getMessage());
            return ResponseEntity.status(500).body("Error importing file: " + e.getMessage());
        }
    }

    private String actor(Principal principal) {
        return principal != null ? principal.getName() : "UNKNOWN";
    }
}
