package com.akosgyongyosi.cashflow;

import com.akosgyongyosi.cashflow.service.CsvImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CashflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashflowApplication.class, args);
    }

	

	/*@Bean
	public CommandLineRunner run(CsvImportService csvImportService) {
		return args -> {
			if (System.getProperty("spring.test.context") == null) { // Skip in tests
				System.out.println("Starting CSV import...");
				csvImportService.importCsvFiles();
				System.out.println("CSV import finished.");
			}
		};
	}*/
	
}
