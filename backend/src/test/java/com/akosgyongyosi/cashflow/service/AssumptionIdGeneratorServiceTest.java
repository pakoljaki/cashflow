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
    void getNextAssumptionId_shouldCreateNewSequenceWhenNotExists() {
        when(repo.findById("ASSUMPTION_ID")).thenReturn(Optional.empty());

        Long result = svc.getNextAssumptionId();

        assertThat(result).isEqualTo(1L);
        
        ArgumentCaptor<AssumptionIdSequence> captor = ArgumentCaptor.forClass(AssumptionIdSequence.class);
        verify(repo, times(2)).save(captor.capture());
        
        AssumptionIdSequence firstSave = captor.getAllValues().get(0);
        assertThat(firstSave.getSeqName()).isEqualTo("ASSUMPTION_ID");
        
        AssumptionIdSequence secondSave = captor.getAllValues().get(1);
        assertThat(secondSave.getSeqName()).isEqualTo("ASSUMPTION_ID");
    }

    @Test
    void getNextAssumptionId_shouldIncrementExistingSequence() {
        AssumptionIdSequence existingSeq = new AssumptionIdSequence();
        existingSeq.setSeqName("ASSUMPTION_ID");
        existingSeq.setNextVal(5L);
        when(repo.findById("ASSUMPTION_ID")).thenReturn(Optional.of(existingSeq));

        Long result = svc.getNextAssumptionId();

        assertThat(result).isEqualTo(5L);
        assertThat(existingSeq.getNextVal()).isEqualTo(6L);
        verify(repo).save(existingSeq);
        verify(repo, times(1)).save(any());
    }

    @Test
    void getNextAssumptionId_shouldHandleMultipleConsecutiveCalls() {
        when(repo.findById("ASSUMPTION_ID"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(createSequence(2L)))
            .thenReturn(Optional.of(createSequence(3L)));

        Long id1 = svc.getNextAssumptionId();
        assertThat(id1).isEqualTo(1L);

        Long id2 = svc.getNextAssumptionId();
        assertThat(id2).isEqualTo(2L);

        Long id3 = svc.getNextAssumptionId();
        assertThat(id3).isEqualTo(3L);

        verify(repo, times(3)).findById("ASSUMPTION_ID");
    }

    @Test
    void getNextAssumptionId_shouldHandleLargeSequenceNumbers() {
        AssumptionIdSequence largeSeq = new AssumptionIdSequence();
        largeSeq.setSeqName("ASSUMPTION_ID");
        largeSeq.setNextVal(999999L);
        when(repo.findById("ASSUMPTION_ID")).thenReturn(Optional.of(largeSeq));

        Long result = svc.getNextAssumptionId();

        assertThat(result).isEqualTo(999999L);
        assertThat(largeSeq.getNextVal()).isEqualTo(1000000L);
        verify(repo).save(largeSeq);
    }

    private AssumptionIdSequence createSequence(Long nextVal) {
        AssumptionIdSequence seq = new AssumptionIdSequence();
        seq.setSeqName("ASSUMPTION_ID");
        seq.setNextVal(nextVal);
        return seq;
    }
}
