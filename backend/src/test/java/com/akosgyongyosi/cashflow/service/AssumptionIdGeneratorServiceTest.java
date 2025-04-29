package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.AssumptionIdSequence;
import com.akosgyongyosi.cashflow.repository.AssumptionIdSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AssumptionIdGeneratorServiceTest {

    private AssumptionIdSequenceRepository repo;
    private AssumptionIdGeneratorService svc;

    @BeforeEach
    void setUp() {
        repo = mock(AssumptionIdSequenceRepository.class);
        svc = new AssumptionIdGeneratorService(repo);
    }

    @Test
    void firstCall_createsSequenceAndReturnsOne() {
        when(repo.findById("ASSUMPTION_ID")).thenReturn(Optional.empty());
        ArgumentCaptor<AssumptionIdSequence> cap = ArgumentCaptor.forClass(AssumptionIdSequence.class);

        long val = svc.getNextAssumptionId();
        assertThat(val).isEqualTo(1L);

        verify(repo, times(2)).save(cap.capture());
        AssumptionIdSequence created = cap.getAllValues().get(0);
        assertThat(created.getNextVal()).isEqualTo(1L);
    }

    @Test
    void subsequentCall_incrementsExisting() {
        AssumptionIdSequence seq = new AssumptionIdSequence();
        seq.setSeqName("ASSUMPTION_ID");
        seq.setNextVal(5L);
        when(repo.findById("ASSUMPTION_ID")).thenReturn(Optional.of(seq));

        long val = svc.getNextAssumptionId();
        assertThat(val).isEqualTo(5L);
        assertThat(seq.getNextVal()).isEqualTo(6L);
        verify(repo).save(seq);
    }
}
