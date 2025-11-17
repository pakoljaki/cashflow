package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.BankAccountRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class CsvImportService {

    @Value("${csv.import.path:csv_imports}")
    private String csvImportPath;

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionCategoryRepository categoryRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final char CSV_DELIMITER = ';';

    public CsvImportService(TransactionRepository transactionRepository,
                            BankAccountRepository bankAccountRepository,
                            TransactionCategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void importCsvFiles() {
        Path dir = Paths.get(csvImportPath);
        if (!Files.isDirectory(dir)) {
            log.warn("CSV directory not found: {}", dir.toAbsolutePath());
            return;
        }

        try {
            Files.list(dir)
                 .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                 .filter(p -> !p.getFileName().toString().contains("-processed"))
                 .forEach(this::processFile);
        } catch (IOException e) {
            throw new CsvImportException("Error scanning CSV directory " + dir, e);
        }
    }

    private void processFile(Path file) {
        String fileName = file.getFileName().toString();
        Currency fileCurrency = parseCurrencyFromFilename(fileName);
        log.info("Processing file {} [{}]", fileName, fileCurrency);

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            int saved = parseCsv(reader, fileCurrency);
            log.info("✓ {} transactions imported from {}", saved, fileName);
            renameAsProcessed(file);
        } catch (IOException e) {
            throw new CsvImportException("Error reading " + file, e);
        }
    }

    @Transactional
    public void parseSingleFile(InputStream in, String originalFilename) {
        Currency fileCurrency = parseCurrencyFromFilename(
                originalFilename == null ? "" : originalFilename);

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            int saved = parseCsv(reader, fileCurrency);
            log.info("✓ {} transactions imported from upload {}", saved, originalFilename);
        } catch (IOException e) {
            log.error("IOException during CSV parsing: {}", e.getMessage(), e);
            throw new CsvImportException("Error parsing uploaded CSV " + originalFilename, e);
        } catch (Exception e) {
            log.error("Unexpected error during CSV import: {}", e.getMessage(), e);
            throw new CsvImportException("Error importing CSV " + originalFilename, e);
        }
    }

    private int parseCsv(BufferedReader reader, Currency fileCurrency) throws IOException {
        int recordCount = 0;

        CSVFormat fmt = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setDelimiter(CSV_DELIMITER)
                .build();
        try (CSVParser parser = new CSVParser(reader, fmt)) {
            for (CSVRecord rec : parser) {
                String first = rec.get(0);
                if (first != null) first = first.trim();
                // Skip potential header (non-date first cell)
                if (first == null || first.isBlank()) {
                    continue; // empty line
                }
                if (!first.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) {
                    // Treat as header ONLY if record number == 1 and contains non-date text
                    if (rec.getRecordNumber() == 1) {
                        continue; // header row
                    }
                }
                Transaction tx = mapToTransaction(rec, fileCurrency);
                if (tx != null) {
                    transactionRepository.save(tx);
                    recordCount++;
                }
            }
        }
        return recordCount;
    }

    private Transaction mapToTransaction(CSVRecord r, Currency fileCurrency) {
        try {
            LocalDate bookingDate = LocalDate.parse(r.get(0).trim(), DATE_FMT);
            LocalDate valueDate   = LocalDate.parse(r.get(1).trim(), DATE_FMT);
            String ourAcctNum     = r.get(2).trim();
            String partnerName    = r.get(3).trim();
            String partnerAcct    = r.get(4).trim();
            BigDecimal amount     = parseAmount(r.get(5).trim());
            TransactionDirection direction =
                    "T".equalsIgnoreCase(r.get(6)) ? TransactionDirection.NEGATIVE
                                                   : TransactionDirection.POSITIVE;
            String transactionCode = r.get(7);
            String memo = r.size() > 8 ? r.get(8) : "";
            // Optional currency override column (index 9)
            Currency rowCurrency = resolveRowCurrency(r, fileCurrency);

            // Use helper (non transactional private) to create/find bank account
            BankAccount ourAccount = findOrCreateBankAccount(ourAcctNum, "Our Company", rowCurrency, null);
            if (ourAccount == null) {
                log.warn("Skipping line (empty ourAccountNumber): {}", r);
                return null;
            }
            
            Transaction tx = new Transaction();
            tx.setAccount(ourAccount);
            tx.setBookingDate(bookingDate);
            tx.setValueDate(valueDate);
            tx.setPartnerName(partnerName);
            tx.setPartnerAccountNumber(partnerAcct);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                amount = amount.abs();
            }
            tx.setAmount(amount);
            tx.setCurrency(rowCurrency);
            tx.setTransactionDirection(direction);
            tx.setTransactionCode(transactionCode);
            tx.setMemo(memo);
            tx.setTransactionMethod(TransactionMethod.TRANSFER);

            // Try to match category by memo, but don't fail if not found
            try {
                categoryRepository.findByName(memo)
                    .ifPresentOrElse(tx::setCategory,
                        () -> tx.setCategory(assignUnassignedCategory(direction)));
            } catch (Exception e) {
                log.warn("Failed to assign category for memo '{}', using unassigned: {}", memo, e.getMessage());
                tx.setCategory(assignUnassignedCategory(direction));
            }

            return tx;

        } catch (Exception ex) {
            log.error("Skipping malformed CSV line: {}", r, ex);
            return null;
        }
    }

    private BigDecimal parseAmount(String raw) {
        return new BigDecimal(raw.replace(",", "."));
    }

    // Removed @Transactional (private helper)
    // Finds existing bank account or creates new one. If existing account has different currency,
    // uses the existing account's currency (database is authoritative).
    private BankAccount findOrCreateBankAccount(String accountNumber,
                                                String owner,
                                                Currency currency,
                                                String bankName) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        try {
            return bankAccountRepository.findByAccountNumber(accountNumber)
                       .map(existing -> {
                           // Bank account already exists - use it as-is
                           // Log warning if currency differs from CSV
                           if (existing.getCurrency() != currency) {
                               log.warn("Bank account {} exists with currency {}, CSV specified {}. Using existing account.",
                                   accountNumber, existing.getCurrency(), currency);
                           }
                           return existing;
                       })
                       .orElseGet(() -> {
                           // Create new bank account
                           BankAccount b = new BankAccount();
                           b.setAccountNumber(accountNumber);
                           b.setOwner(owner);
                           b.setBankName(bankName);
                           b.setCurrency(currency);
                           log.info("Creating new bank account: {} [{}]", accountNumber, currency);
                           return bankAccountRepository.save(b);
                       });
        } catch (Exception e) {
            log.error("Error finding/creating bank account {}: {}", accountNumber, e.getMessage());
            throw e;
        }
    }

    // Removed @Transactional (private helper)
    private TransactionCategory assignUnassignedCategory(TransactionDirection dir) {
        String name = "Unassigned(" + dir.name() + ")";
        try {
            return categoryRepository.findByName(name)
                    .orElseGet(() -> {
                        TransactionCategory c = new TransactionCategory();
                        c.setName(name);
                        c.setDirection(dir);
                        c.setDescription("Auto‑created unassigned category for " + dir);
                        return categoryRepository.save(c);
                    });
        } catch (Exception e) {
            log.error("Error finding/creating unassigned category: {}", e.getMessage());
            // Try to find it again in case it was created by another thread
            return categoryRepository.findByName(name)
                    .orElseThrow(() -> new RuntimeException("Cannot create unassigned category", e));
        }
    }

    private Currency resolveRowCurrency(CSVRecord r, Currency fileCurrency) {
        if (r.size() <= 9) return fileCurrency;
        String rawCur = r.get(9);
        if (rawCur == null || rawCur.isBlank()) return fileCurrency;
        String norm = rawCur.trim().toUpperCase(Locale.ROOT);
        try {
            return Currency.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown currency '{}' in CSV line; falling back to file currency {}", rawCur, fileCurrency);
            return fileCurrency;
        }
    }

    // Custom exception wrapping IO issues during CSV import
    public static class CsvImportException extends RuntimeException {
        public CsvImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private Currency parseCurrencyFromFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.contains("usd")) return Currency.USD;
        if (lower.contains("eur")) return Currency.EUR;
        return Currency.HUF;
    }

    private void renameAsProcessed(Path file) {
        try {
            Path processed = file.resolveSibling(
                    file.getFileName().toString().replace(".csv", "-processed.csv"));
            Files.move(file, processed, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.warn("Could not rename {} as processed: {}", file, ex.getMessage());
        }
    }
}
