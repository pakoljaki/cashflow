package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.service.CsvImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CsvUploadControllerTest {

    @Mock
    private CsvImportService csvImportService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private CsvUploadController csvUploadController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(csvUploadController, "importDir", "csv_imports");
    }

    @Test
    void uploadCsvFile_shouldReturnSuccessWhenFileIsParsed() throws Exception {
        String csvContent = "date,description,amount\n2024-01-01,Test,100.00";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        doNothing().when(csvImportService).parseSingleFile(any(InputStream.class), eq("test.csv"));

        ResponseEntity<?> response = csvUploadController.uploadCsvFile(multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("File parsed successfully in memory.");
        verify(csvImportService).parseSingleFile(any(InputStream.class), eq("test.csv"));
    }

    @Test
    void uploadCsvFile_shouldReturnBadRequestWhenFileIsEmpty() {
        when(multipartFile.isEmpty()).thenReturn(true);

        ResponseEntity<?> response = csvUploadController.uploadCsvFile(multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("No file uploaded");
        verify(csvImportService, never()).parseSingleFile(any(), any());
    }

    @Test
    void uploadCsvFile_shouldReturnErrorWhenParsingFails() throws Exception {
        String csvContent = "invalid csv content";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("invalid.csv");
        doThrow(new RuntimeException("Parse error")).when(csvImportService)
            .parseSingleFile(any(InputStream.class), eq("invalid.csv"));

        ResponseEntity<?> response = csvUploadController.uploadCsvFile(multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().toString()).contains("Error parsing file");
    }
}
