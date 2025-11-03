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
    void getAllTransactions_shouldReturnAllTransactionsFromRepository() {
        Transaction t1 = new Transaction();
        t1.setId(1L);
        Transaction t2 = new Transaction();
        t2.setId(2L);
        when(transactionRepository.findAll()).thenReturn(List.of(t1, t2));

        List<Transaction> result = service.getAllTransactions();

        assertThat(result)
            .hasSize(2)
            .containsExactly(t1, t2);
        verify(transactionRepository).findAll();
    }

    @Test
    void getAllTransactions_shouldReturnEmptyListWhenNoTransactions() {
        when(transactionRepository.findAll()).thenReturn(List.of());

        List<Transaction> result = service.getAllTransactions();

        assertThat(result).isEmpty();
        verify(transactionRepository).findAll();
    }

    @Test
    void saveTransaction_shouldCallRepositorySave() {
        Transaction tx = new Transaction();
        tx.setId(1L);

        service.saveTransaction(tx);

        verify(transactionRepository).save(tx);
    }

    @Test
    void saveTransaction_shouldHandleNewTransaction() {
        Transaction tx = new Transaction();
        // No ID set - new transaction

        service.saveTransaction(tx);

        verify(transactionRepository).save(tx);
    }

    @Test
    void getAllTransactions_shouldHandleLargeList() {
        List<Transaction> largeList = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Transaction t = new Transaction();
            t.setId((long) i);
            largeList.add(t);
        }
        when(transactionRepository.findAll()).thenReturn(largeList);

        List<Transaction> result = service.getAllTransactions();

        assertThat(result).hasSize(100);
        verify(transactionRepository).findAll();
    }
}
