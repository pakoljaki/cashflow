package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.BankAccount;
import com.akosgyongyosi.cashflow.entity.CurrencyType;
import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import com.akosgyongyosi.cashflow.repository.BankAccountRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.akosgyongyosi.cashflow.entity.TransactionMethod;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;


import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class CsvImportService {

    @Value("${csv.import.path}")
    private String csvImportPath;

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionCategoryRepository categoryRepository;

    public CsvImportService(TransactionRepository transactionRepository,
                            BankAccountRepository bankAccountRepository,
                            TransactionCategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void importCsvFiles() {
        try {
            // The backend's working directory is typically "backend/", so go one level up.
            Path csvDir = Paths.get(System.getProperty("user.dir"))
                               .getParent()
                               .resolve("csv_imports");

            System.out.println("Working directory: " + System.getProperty("user.dir"));
            if (!Files.exists(csvDir) || !Files.isDirectory(csvDir)) {
                System.err.println("CSV directory not found: " + csvDir);
                return;
            }

            System.out.println("CSV directory resolved to: " + csvDir);

            long fileCount = Files.list(csvDir)
                .filter(Files::isRegularFile)
                // Use case-insensitive check for .csv extension
                .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".csv"))
                // Skip already processed files
                .filter(file -> !file.getFileName().toString().contains("-processed"))
                .peek(file -> System.out.println("Found file: " + file.getFileName()))
                .peek(this::processFile)
                .count();

            System.out.println("Total CSV files found: " + fileCount);

        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV directory", e);
        }
    }

    private void processFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        System.out.println("Processing file: " + fileName);

        // Debug: Print the first 5 lines
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            System.out.println("First 5 lines of file:");
            reader.lines().limit(5).forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Error reading raw file: " + fileName);
        }

        // Determine the currency from the filename (e.g. "HUF" or "EUR")
        CurrencyType fileCurrency = parseCurrencyFromFilename(fileName);

        // Parse the CSV records
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(';'))) {

            int recordCount = 0;
            for (CSVRecord record : csvParser) {
                System.out.println("Read record: " + record);

                // Create a transaction from the record
                Transaction transaction = mapToTransaction(record, fileCurrency);
                if (transaction != null) {
                    transactionRepository.save(transaction);
                    recordCount++;
                }
            }
            System.out.println("Total records saved: " + recordCount);

            renameFileAsProcessed(filePath);
            System.out.println("Processed file: " + fileName);

        } catch (IOException e) {
            throw new RuntimeException("Error processing file: " + fileName, e);
        }
    }

    /**
     * Create a Transaction from a CSVRecord, automatically creating any BankAccounts that don't exist.
     */
    private Transaction mapToTransaction(CSVRecord record, CurrencyType fileCurrency) {
        try {
            System.out.println("Mapping record: " + record);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

            // CSV Columns (example):
            //  0: bookingDate
            //  1: valueDate
            //  2: ourAccountNumber
            //  3: partnerName
            //  4: partnerAccountNumber
            //  5: amount
            //  6: T or J
            //  7: transactionCode
            //  8: memo

            LocalDate bookingDate = LocalDate.parse(record.get(0), formatter);
            LocalDate valueDate = LocalDate.parse(record.get(1), formatter);
            String ourAccountNumber = record.get(2);
            String partnerName = record.get(3);
            String partnerAccountNumber = record.get(4);

            BigDecimal amount = new BigDecimal(record.get(5).replace(",", "."));
            TransactionDirection transactionDirection = "T".equals(record.get(6))
                    ? TransactionDirection.NEGATIVE
                    : TransactionDirection.POSITIVE;

            String transactionCode = record.get(7);
            String memo = record.size() > 8 ? record.get(8) : "";  // In case some lines have fewer columns

            System.out.println("Parsed fields: " + bookingDate + ", " + valueDate + ", " + ourAccountNumber + ", " + amount);

            // 1) Find or create the "our" account (3rd column)
            //    We'll use a fixed "owner" name, e.g. "Our Company"
            //    or you can store something else as bankName/owner as needed.
            BankAccount ourAccount = findOrCreateBankAccount(
                ourAccountNumber,
                "Our Company",
                fileCurrency,
                null // no explicit bankName for our own account
            );
            if (ourAccount == null) {
                // If our accountNumber is missing, skip this transaction
                System.err.println("Skipping transaction because our account is null or empty.");
                return null;
            }

            // 2) Find or create the partner’s account (5th column), only if not empty
            BankAccount partnerAccount = null;
            if (partnerAccountNumber != null && !partnerAccountNumber.isBlank()) {
                // We'll store the partner's name in "owner", optionally set bankName to partnerName too
                partnerAccount = findOrCreateBankAccount(
                    partnerAccountNumber,
                    partnerName.isBlank() ? null : partnerName,
                    fileCurrency,
                    partnerName
                );
            }

            // 3) Create the Transaction object
            //    We'll store ourAccount as the "account" in Transaction
            //    The "partner" field is not in Transaction by default, but you can add it if needed
            Transaction transaction = new Transaction();
            transaction.setAccount(ourAccount);
            transaction.setBookingDate(bookingDate);
            transaction.setValueDate(valueDate);
            transaction.setPartnerName(partnerName);
            transaction.setPartnerAccountNumber(partnerAccountNumber);
            transaction.setAmount(amount);
            transaction.setCurrency(fileCurrency);
            transaction.setTransactionDirection(transactionDirection);
            transaction.setTransactionCode(transactionCode);
            transaction.setMemo(memo);
            transaction.setTransactionMethod(TransactionMethod.TRANSFER); //auto set here

            // 4) If you want a category, look it up by "memo" or something else
            TransactionCategory category = categoryRepository.findByName(memo).orElse(null);
            transaction.setCategory(category);

            return transaction;

        } catch (Exception e) {
            System.err.println("Skipping invalid transaction record: " + record);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Finds or creates a BankAccount with the given accountNumber. 
     * - If accountNumber is empty, returns null.
     * - The currency is set from the CSV filename.
     * - The owner or bankName can be set if provided.
     */
    private BankAccount findOrCreateBankAccount(String accountNumber,
                                                String owner,
                                                CurrencyType currency,
                                                String bankName) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }

        // Try to find an existing account
        Optional<BankAccount> existingOpt = bankAccountRepository.findByAccountNumber(accountNumber);
        if (existingOpt.isPresent()) {
            // If found, return it
            return existingOpt.get();
        }

        // Otherwise, create a new one
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber(accountNumber);
        newAccount.setOwner(owner);         // could be "Our Company" or partnerName
        newAccount.setBankName(bankName);   // optional
        newAccount.setCurrency(currency);    // from filename
        // Save
        return bankAccountRepository.save(newAccount);
    }

    /**
     * Parse the currency from the filename, e.g. "11_HUF_2024_01-02.csv" => HUF
     * If none found, default to HUF or handle as needed.
     */
    private CurrencyType parseCurrencyFromFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.contains("eur")) {
            return CurrencyType.EUR;
        } else {
            return CurrencyType.HUF;  // fallback
        }
    }

    /**
     * Parses a single CSV file in-memory from the given InputStream,
     * builds Transaction entities, and saves them to the database.
     */
    public void parseSingleFile(InputStream inputStream, String originalFilename) throws Exception {
        // Example approach: parse the CSV lines using Commons CSV in memory
        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(';'))) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            int recordCount = 0;

            for (CSVRecord record : csvParser) {
                // Example columns: bookingDate, valueDate, ourAcct, partnerName, partnerAcct, amount, T/J, code, memo
                try {
                    LocalDate bookingDate = LocalDate.parse(record.get(0), formatter);
                    LocalDate valueDate   = LocalDate.parse(record.get(1), formatter);
                    String ourAccountNumber = record.get(2);
                    String partnerName   = record.get(3);
                    String partnerAcct   = record.get(4);
                    BigDecimal amount    = new BigDecimal(record.get(5).replace(",", "."));

                    TransactionDirection direction =
                        "T".equals(record.get(6)) ? TransactionDirection.NEGATIVE : TransactionDirection.POSITIVE;

                    String transactionCode = record.get(7);
                    String memo = (record.size() > 8) ? record.get(8) : "";

                    // find or create bank account
                    Optional<BankAccount> ourAcctOpt = bankAccountRepository.findByAccountNumber(ourAccountNumber);
                    if (ourAcctOpt.isEmpty()) {
                        // skip if we can't find or create the account
                        System.err.println("Skipping row, account not found: " + ourAccountNumber);
                        continue;
                    }
                    BankAccount ourAccount = ourAcctOpt.get();

                    // build the transaction
                    Transaction tx = new Transaction();
                    tx.setAccount(ourAccount);
                    tx.setBookingDate(bookingDate);
                    tx.setValueDate(valueDate);
                    tx.setPartnerName(partnerName);
                    tx.setPartnerAccountNumber(partnerAcct);
                    tx.setAmount(amount);
                    tx.setCurrency(ourAccount.getCurrency());  // or parse from filename if needed
                    tx.setTransactionDirection(direction);
                    tx.setTransactionCode(transactionCode);
                    tx.setMemo(memo);

                    // if you have transactionMethod
                    tx.setTransactionMethod(TransactionMethod.TRANSFER);

                    // optional category
                    TransactionCategory category = categoryRepository.findByName(memo).orElse(null);
                    tx.setCategory(category);

                    // save transaction
                    transactionRepository.save(tx);
                    recordCount++;
                } catch (Exception rowEx) {
                    System.err.println("Skipping invalid row: " + record);
                    rowEx.printStackTrace();
                }
            }

            System.out.println("parseSingleFile: " + recordCount + " transactions saved from " + originalFilename);
        }
    }

    /**
     * Renames the file from "somefile.csv" to "somefile-processed.csv" after processing.
     */
    private void renameFileAsProcessed(Path filePath) {
        /*try {
            Path newPath = filePath.getParent().resolve(
                filePath.getFileName().toString().replace(".csv", "-processed.csv")
            );
            Files.move(filePath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error renaming file: " + filePath);
        }*/
    }
}
