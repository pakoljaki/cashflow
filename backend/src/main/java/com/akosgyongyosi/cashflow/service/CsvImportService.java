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
import java.util.Optional;

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
            throw new RuntimeException("Error scanning CSV directory " + dir, e);
        }
    }

    private void processFile(Path file) {
        String fileName = file.getFileName().toString();
        CurrencyType fileCurrency = parseCurrencyFromFilename(fileName);
        log.info("Processing file {} [{}]", fileName, fileCurrency);

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            int saved = parseCsv(reader, fileCurrency);
            log.info("✓ {} transactions imported from {}", saved, fileName);
            renameAsProcessed(file);
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + file, e);
        }
    }

    @Transactional
    public void parseSingleFile(InputStream in, String originalFilename) {
        CurrencyType fileCurrency = parseCurrencyFromFilename(
                originalFilename == null ? "" : originalFilename);

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            int saved = parseCsv(reader, fileCurrency);
            log.info("✓ {} transactions imported from upload {}", saved, originalFilename);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing uploaded CSV " + originalFilename, e);
        }
    }

    private int parseCsv(BufferedReader reader, CurrencyType fileCurrency) throws IOException {
        int recordCount = 0;

        try (CSVParser parser = new CSVParser(reader,
                CSVFormat.DEFAULT.withDelimiter(CSV_DELIMITER))) {

            for (CSVRecord record : parser) {
                Transaction tx = mapToTransaction(record, fileCurrency);
                if (tx != null) {
                    transactionRepository.save(tx);
                    recordCount++;
                }
            }
        }
        return recordCount;
    }

    private Transaction mapToTransaction(CSVRecord r, CurrencyType fileCurrency) {
        try {
            LocalDate bookingDate = LocalDate.parse(r.get(0), DATE_FMT);
            LocalDate valueDate   = LocalDate.parse(r.get(1), DATE_FMT);
            String ourAcctNum     = r.get(2);
            String partnerName    = r.get(3);
            String partnerAcct    = r.get(4);
            BigDecimal amount     = parseAmount(r.get(5));
            TransactionDirection direction =
                    "T".equalsIgnoreCase(r.get(6)) ? TransactionDirection.NEGATIVE
                                                   : TransactionDirection.POSITIVE;
            String transactionCode = r.get(7);
            String memo = r.size() > 8 ? r.get(8) : "";

            BankAccount ourAccount = findOrCreateBankAccount(ourAcctNum, "Our Company",
                                                             fileCurrency, null);
            if (ourAccount == null) {
                log.warn("Skipping line (empty ourAccountNumber): {}", r);
                return null;
            }
            BankAccount partnerAccount = partnerAcct == null || partnerAcct.isBlank()
                                         ? null
                                         : findOrCreateBankAccount(partnerAcct,
                                                                   partnerName.isBlank() ? null : partnerName,
                                                                   fileCurrency,
                                                                   partnerName);

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
            tx.setCurrency(fileCurrency);
            tx.setTransactionDirection(direction);
            tx.setTransactionCode(transactionCode);
            tx.setMemo(memo);
            tx.setTransactionMethod(TransactionMethod.TRANSFER);

            categoryRepository.findByName(memo)
                              .ifPresentOrElse(tx::setCategory,
                                               () -> tx.setCategory(assignUnassignedCategory(direction)));

            return tx;

        } catch (Exception ex) {
            log.error("Skipping malformed CSV line: {}", r, ex);
            return null;
        }
    }

    private BigDecimal parseAmount(String raw) {
        return new BigDecimal(raw.replace(",", "."));
    }

    @Transactional
    private BankAccount findOrCreateBankAccount(String accountNumber,
                                                String owner,
                                                CurrencyType currency,
                                                String bankName) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        return bankAccountRepository.findByAccountNumber(accountNumber)
                   .orElseGet(() -> {
                       BankAccount b = new BankAccount();
                       b.setAccountNumber(accountNumber);
                       b.setOwner(owner);
                       b.setBankName(bankName);
                       b.setCurrency(currency);
                       return bankAccountRepository.save(b);
                   });
    }

    @Transactional
    private TransactionCategory assignUnassignedCategory(TransactionDirection dir) {
        String name = "Unassigned(" + dir.name() + ")";
        return categoryRepository.findByName(name)
                .orElseGet(() -> {
                    TransactionCategory c = new TransactionCategory();
                    c.setName(name);
                    c.setDirection(dir);
                    c.setDescription("Auto‑created unassigned category for " + dir);
                    return categoryRepository.save(c);
                });
    }

    private CurrencyType parseCurrencyFromFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.contains("eur") ? CurrencyType.EUR : CurrencyType.HUF;
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
