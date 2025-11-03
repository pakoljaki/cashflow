package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.service.CsvImportService.CsvImportException;
import com.akosgyongyosi.cashflow.repository.BankAccountRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.akosgyongyosi.cashflow.entity.*;
import org.mockito.ArgumentCaptor;

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


    private BankAccount mockBankAccount;
    private TransactionCategory mockCategory;

    @BeforeEach
    void setUp() {
        mockBankAccount = new BankAccount();
        mockBankAccount.setId(1L);
        mockBankAccount.setAccountNumber("123");
        mockBankAccount.setCurrency(Currency.HUF);

        mockCategory = new TransactionCategory();
        mockCategory.setId(1L);
        mockCategory.setName("Unassigned(NEGATIVE)");
        mockCategory.setDirection(TransactionDirection.NEGATIVE);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldParseBasicCsvWithTwoTransactions() {
        String content = """
            2025.01.01;2025.01.02;123;Partner A;456;100,50;T;CODE1;memo1
            2025.02.03;2025.02.04;123;Partner B;789;200,75;J;CODE2;memo2
            """;
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber("123")).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample_huf.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        
        assertThat(captor.getAllValues())
            .hasSize(2)
            .extracting(Transaction::getPartnerName)
            .containsExactly("Partner A", "Partner B");
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldParseCurrencyFromFilename() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "transactions_eur.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.EUR);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldHandleOptionalCurrencyColumn() {
        String content = """
            2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo1;EUR
            2025.02.03;2025.02.04;123;Partner;456;200,75;J;CODE2;;USD
            """;
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample_huf.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0).getCurrency()).isEqualTo(Currency.EUR);
        assertThat(captor.getAllValues().get(1).getCurrency()).isEqualTo(Currency.USD);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldSkipHeaderRow() {
        String content = """
            BookingDate;ValueDate;Account;Partner;PartnerAcct;Amount;Direction;Code;Memo
            2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo
            """;
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        verify(transactionRepository, times(1)).save(any()); // Only 1 data row
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldHandleNegativeDirection() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionDirection()).isEqualTo(TransactionDirection.NEGATIVE);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldHandlePositiveDirection() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;J;CODE;memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        
        TransactionCategory positiveCategory = new TransactionCategory();
        positiveCategory.setName("Unassigned(POSITIVE)");
        positiveCategory.setDirection(TransactionDirection.POSITIVE);
        
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(positiveCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionDirection()).isEqualTo(TransactionDirection.POSITIVE);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldReuseExistingBankAccount() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        BankAccount existingAccount = new BankAccount();
        existingAccount.setId(99L);
        existingAccount.setAccountNumber("123");
        existingAccount.setCurrency(Currency.HUF);

        when(bankAccountRepository.findByAccountNumber("123")).thenReturn(Optional.of(existingAccount));
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        verify(bankAccountRepository, never()).save(any()); // Should not create new account
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAccount()).isEqualTo(existingAccount);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldHandleEmptyLines() {
        String content = """
            2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo
            
            2025.02.03;2025.02.04;123;Partner;789;200,75;J;CODE2;memo2
            """;
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        verify(transactionRepository, times(2)).save(any()); // Should skip empty line
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldConvertAmountWithComma() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;1234,56;T;CODE;memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("1234.56");
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldFallbackToHufWhenInvalidCurrencyInColumn() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo;INVALID";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample_huf.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.HUF);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldSetTransactionMethod() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionMethod()).isEqualTo(TransactionMethod.TRANSFER);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldHandleMissingOptionalMemoField() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getMemo()).isEqualTo("");
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldMatchCategoryByMemoWhenExists() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;SalaryIncome";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        TransactionCategory existingCategory = new TransactionCategory();
        existingCategory.setId(99L);
        existingCategory.setName("SalaryIncome");

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName("SalaryIncome")).thenReturn(Optional.of(existingCategory));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(existingCategory);
        verify(categoryRepository, never()).save(any()); // Should not create new category
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldCreateUnassignedCategoryWhenMemoNotMatched() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;UnknownMemo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        TransactionCategory unassignedCategory = new TransactionCategory();
        unassignedCategory.setName("Unassigned(NEGATIVE)");

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName("UnknownMemo")).thenReturn(Optional.empty());
        when(categoryRepository.findByName("Unassigned(NEGATIVE)")).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(unassignedCategory);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        verify(categoryRepository).save(any(TransactionCategory.class));
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(unassignedCategory);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldHandleMalformedCsvGracefully() {
        String content = "BADDATE;2025.01.02;123;Partner;456;100,50;T;CODE;Memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        service.parseSingleFile(in, "sample.csv");

        verify(transactionRepository, never()).save(any());
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldThrowCsvImportExceptionOnException() {
        // Use a corrupted stream that will cause parsing errors
        InputStream badStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        // which gets caught by the general Exception handler
        assertThatThrownBy(() -> service.parseSingleFile(badStream, "error.csv"))
            .isInstanceOf(CsvImportException.class)
            .hasMessageContaining("Error importing CSV error.csv");
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldSkipLineWithEmptyOurAccountNumber() {
        String content = "2025.01.01;2025.01.02;;Partner;456;100,50;T;CODE;Memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        service.parseSingleFile(in, "sample.csv");

        verify(transactionRepository, never()).save(any());
        verify(bankAccountRepository, never()).save(any());
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldSkipHeaderRowAndEmptyLines() {
        String content = "Date;ValueDate;Account;Partner;PartnerAcct;Amount;Direction;Code;Memo\n" +
                         "\n" +  // Empty line
                         "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;Memo\n" +
                         "   \n"; // Blank line with spaces

        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        verify(transactionRepository, times(1)).save(any());
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldHandleNegativeAmount() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;-100,50;T;CODE;Memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("100.50");
        assertThat(captor.getValue().getTransactionDirection()).isEqualTo(TransactionDirection.NEGATIVE);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldWarnWhenExistingAccountHasDifferentCurrency() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;Memo";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        BankAccount existingAccount = new BankAccount();
        existingAccount.setId(1L);
        existingAccount.setAccountNumber("123");
        existingAccount.setCurrency(Currency.HUF); // Different from file currency (EUR)

        when(bankAccountRepository.findByAccountNumber("123")).thenReturn(Optional.of(existingAccount));
        when(categoryRepository.findByName(any())).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample_EUR.csv"); // Filename indicates EUR

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAccount()).isEqualTo(existingAccount);
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.EUR); // Transaction uses file currency
        verify(bankAccountRepository, never()).save(any()); // Should not create new account
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldHandleCurrencyOverrideColumn() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;Memo;USD";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample_EUR.csv"); // File says EUR but row overrides to USD

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.USD);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldFallbackToFileCurrencyWhenRowCurrencyInvalid() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;Memo;INVALID";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample_EUR.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.EUR);
    }

    @org.junit.jupiter.api.Test
    void parseSingleFile_shouldUseFileCurrencyWhenCurrencyColumnEmpty() {
        String content = "2025.01.01;2025.01.02;123;Partner;456;100,50;T;CODE;Memo;";
        InputStream in = new ByteArrayInputStream(content.getBytes());

        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenReturn(mockBankAccount);
        when(categoryRepository.findByName(any())).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parseSingleFile(in, "sample_HUF.csv");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.HUF);
    }
}
