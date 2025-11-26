package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.service.CsvImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/csv")
public class CsvUploadController {

    private final CsvImportService csvImportService;

    @Value("${app.csv.import-dir:csv_imports}")
    private String importDir;

    public CsvUploadController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsvFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }
        try (InputStream inputStream = file.getInputStream()) {
            csvImportService.parseSingleFile(inputStream, file.getOriginalFilename());
            return ResponseEntity.ok("File parsed successfully in memory.");
        } catch (Exception e) {
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
    public ResponseEntity<?> importExisting(@RequestParam("file") String fileName) {
        Path filePath = Paths.get(importDir, fileName);
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            return ResponseEntity.badRequest().body("File not found: " + fileName);
        }
        try (InputStream in = Files.newInputStream(filePath)) {
            csvImportService.parseSingleFile(in, fileName);
            return ResponseEntity.ok("Imported " + fileName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error importing file: " + e.getMessage());
        }
    }
}
