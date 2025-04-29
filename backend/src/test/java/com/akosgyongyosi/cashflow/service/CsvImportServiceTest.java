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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @InjectMocks
    private CsvImportService service;

    private String csvContent;

    @BeforeEach
    void setUp() {
        csvContent =
            "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo1\n" +
            "2025.02.03;2025.02.04;123;Partner;456;200,75;J;CODE2;\n";
    }

    @Test
    void parseSingleFile_savesValidRecords() throws Exception {
        BankAccount acct = new BankAccount();
        acct.setAccountNumber("123");
        acct.setCurrency(CurrencyType.EUR);
        when(bankAccountRepository.findByAccountNumber("123")).thenReturn(Optional.of(acct));

        TransactionCategory cat1 = new TransactionCategory();
        cat1.setName("memo1");
        when(categoryRepository.findByName("memo1")).thenReturn(Optional.of(cat1));
        when(categoryRepository.findByName("")).thenReturn(Optional.of(cat1));

        InputStream is = new ByteArrayInputStream(csvContent.getBytes());
        service.parseSingleFile(is, "data.csv");

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        Transaction first = captor.getAllValues().get(0);
        assertThat(first.getBookingDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(first.getValueDate()).isEqualTo(LocalDate.of(2025, 1, 2));
        assertThat(first.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(first.getTransactionDirection()).isEqualTo(TransactionDirection.NEGATIVE);
        assertThat(first.getCurrency()).isEqualTo(CurrencyType.EUR);
        assertThat(first.getCategory()).isEqualTo(cat1);
        assertThat(first.getMemo()).isEqualTo("memo1");
        assertThat(first.getTransactionMethod()).isEqualTo(TransactionMethod.TRANSFER);
    }

    @Test
    void parseSingleFile_createsUnassignedCategoryWhenMissing() throws Exception {
        BankAccount acct = new BankAccount();
        acct.setAccountNumber("123");
        acct.setCurrency(CurrencyType.HUF);
        when(bankAccountRepository.findByAccountNumber("123")).thenReturn(Optional.of(acct));
        when(categoryRepository.findByName("memo1")).thenReturn(Optional.empty());

        TransactionCategory savedCat = new TransactionCategory();
        savedCat.setName("Unassigned(NEGATIVE)");
        savedCat.setDirection(TransactionDirection.NEGATIVE);
        when(categoryRepository.save(any())).thenReturn(savedCat);

        String single = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo1";
        InputStream is = new ByteArrayInputStream(single.getBytes());
        service.parseSingleFile(is, "data.csv");

        verify(categoryRepository).save(argThat(c ->
            c.getName().equals("Unassigned(NEGATIVE)")
         && c.getDirection() == TransactionDirection.NEGATIVE
        ));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void parseSingleFile_skipsWhenAccountMissing() throws Exception {
        when(bankAccountRepository.findByAccountNumber("123")).thenReturn(Optional.empty());

        InputStream is = new ByteArrayInputStream(csvContent.getBytes());
        service.parseSingleFile(is, "data.csv");

        verify(transactionRepository, never()).save(any());
    }
}
