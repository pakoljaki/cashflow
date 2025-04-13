package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.BankAccount;
import com.akosgyongyosi.cashflow.entity.CurrencyType;
import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import com.akosgyongyosi.cashflow.entity.TransactionMethod;
import com.akosgyongyosi.cashflow.repository.BankAccountRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
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
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".csv"))
                .filter(file -> !file.getFileName().toString().contains("-processed"))
                .peek(file -> System.out.println("Found file: " + file.getFileName()))
                .peek(this::processFile)
                .count();

            System.out.println("Total CSV files found: " + fileCount);

        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV directory", e);
        }
    }

    @Transactional
    private void processFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        System.out.println("Processing file: " + fileName);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            System.out.println("First 5 lines of file:");
            reader.lines().limit(5).forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Error reading raw file: " + fileName);
        }

        CurrencyType fileCurrency = parseCurrencyFromFilename(fileName);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(';'))) {

            int recordCount = 0;
            for (CSVRecord record : csvParser) {
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

    @Transactional
    private Transaction mapToTransaction(CSVRecord record, CurrencyType fileCurrency) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

            // CSV columns:
            // 0: bookingDate
            // 1: valueDate
            // 2: ourAccountNumber
            // 3: partnerName
            // 4: partnerAccountNumber
            // 5: amount
            // 6: T or J Terhelés or Jóváírás
            // 7: transactionCode
            // 8: memo (optional)

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
            BankAccount ourAcct = findOrCreateBankAccount(ourAccountNumber, "Our Company", fileCurrency, null);
            if (ourAcct == null) {
                System.err.println("Skipping transaction because our account is null/empty.");
                return null;
            }

            BankAccount partnerAccount = null;
            if (partnerAcct != null && !partnerAcct.isBlank()) {
                partnerAccount = findOrCreateBankAccount(partnerAcct, 
                    partnerName.isBlank() ? null : partnerName, 
                    fileCurrency,
                    partnerName
                );
            }

            Transaction tx = new Transaction();
            tx.setAccount(ourAcct);
            tx.setBookingDate(bookingDate);
            tx.setValueDate(valueDate);
            tx.setPartnerName(partnerName);
            tx.setPartnerAccountNumber(partnerAcct);
            tx.setAmount(amount);
            tx.setCurrency(fileCurrency);
            tx.setTransactionDirection(direction);
            tx.setTransactionCode(transactionCode);
            tx.setMemo(memo);
            tx.setTransactionMethod(TransactionMethod.TRANSFER);

            Optional<TransactionCategory> catOpt = categoryRepository.findByName(memo);
            if (catOpt.isPresent()) {
                tx.setCategory(catOpt.get());
            } else {
                tx.setCategory(assignUnassignedCategory(direction));
            }

            return tx;

        } catch (Exception e) {
            System.err.println("Skipping invalid transaction record: " + record);
            e.printStackTrace();
            return null;
        }
    }

  
    @Transactional 
    private TransactionCategory assignUnassignedCategory(TransactionDirection direction) {
        String unassignedName = "Unassigned(" + direction.name() + ")";

        Optional<TransactionCategory> unassignedOpt = categoryRepository.findByName(unassignedName);
        if (unassignedOpt.isPresent()) {
            return unassignedOpt.get();
        }

        TransactionCategory newCat = new TransactionCategory();
        newCat.setName(unassignedName);
        newCat.setDirection(direction);
        newCat.setDescription("Auto-created unassigned category for " + direction);
        return categoryRepository.save(newCat);
    }

    @Transactional
    private BankAccount findOrCreateBankAccount(String accountNumber, 
                                                String owner, 
                                                CurrencyType currency,
                                                String bankName) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        Optional<BankAccount> existingOpt = bankAccountRepository.findByAccountNumber(accountNumber);
        if (existingOpt.isPresent()) {
            return existingOpt.get();
        }
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber(accountNumber);
        newAccount.setOwner(owner);
        newAccount.setBankName(bankName);
        newAccount.setCurrency(currency);
        return bankAccountRepository.save(newAccount);
    }

    private CurrencyType parseCurrencyFromFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.contains("eur")) {
            return CurrencyType.EUR;
        } else {
            return CurrencyType.HUF; 
        }
    }

    public void parseSingleFile(InputStream inputStream, String originalFilename) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(';'))) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            int recordCount = 0;

            for (CSVRecord record : csvParser) {
                try {
                    LocalDate bookingDate = LocalDate.parse(record.get(0), formatter);
                    LocalDate valueDate   = LocalDate.parse(record.get(1), formatter);
                    String ourAcctNum     = record.get(2);
                    String partnerName    = record.get(3);
                    String partnerAcct    = record.get(4);
                    BigDecimal amount     = new BigDecimal(record.get(5).replace(",", "."));

                    TransactionDirection direction =
                        "T".equals(record.get(6)) ? TransactionDirection.NEGATIVE : TransactionDirection.POSITIVE;

                    String transactionCode = record.get(7);
                    String memo = (record.size() > 8) ? record.get(8) : "";

                    Optional<BankAccount> ourAcctOpt = bankAccountRepository.findByAccountNumber(ourAcctNum);
                    if (ourAcctOpt.isEmpty()) {
                        System.err.println("Skipping row, account not found: " + ourAcctNum);
                        continue;
                    }
                    BankAccount ourAcct = ourAcctOpt.get();

                    Transaction tx = new Transaction();
                    tx.setAccount(ourAcct);
                    tx.setBookingDate(bookingDate);
                    tx.setValueDate(valueDate);
                    tx.setPartnerName(partnerName);
                    tx.setPartnerAccountNumber(partnerAcct);
                    tx.setAmount(amount);
                    tx.setTransactionDirection(direction);
                    tx.setTransactionCode(transactionCode);
                    tx.setMemo(memo);
                    tx.setTransactionMethod(TransactionMethod.TRANSFER);
                    tx.setCurrency(ourAcct.getCurrency());

                    Optional<TransactionCategory> catOpt = categoryRepository.findByName(memo);
                    if (catOpt.isPresent()) {
                        tx.setCategory(catOpt.get());
                    } else {
                        tx.setCategory(assignUnassignedCategory(direction));
                    }

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

    @Transactional
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
