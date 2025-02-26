package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.service.CsvImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api/admin/csv")
public class CsvUploadController {

    private final CsvImportService csvImportService;

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
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error parsing file: " + e.getMessage());
        }
    }
}
