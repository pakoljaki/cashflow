package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService service;

    @Test
    void getAllTransactions_returnsListFromRepository() {
        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();
        when(transactionRepository.findAll()).thenReturn(List.of(t1, t2));
        List<Transaction> result = service.getAllTransactions();
        assertThat(result).containsExactly(t1, t2);
        verify(transactionRepository).findAll();
    }

    @Test
    void saveTransaction_callsRepositorySave() {
        Transaction tx = new Transaction();
        service.saveTransaction(tx);
        verify(transactionRepository).save(tx);
    }
}
