package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FxRateEnsurerTest {

    @Mock
    private FxIngestionService ingestion;

    @Mock
    private FxProperties props;

    @InjectMocks
    private FxRateEnsurer fxRateEnsurer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void ensureFor_shouldTriggerIngestionWhenEnabled() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        when(props.isEnabled()).thenReturn(true);

        fxRateEnsurer.ensureFor(date);

        verify(props).isEnabled();
        verify(ingestion).fetchAndUpsert(date);
    }

    @Test
    void ensureFor_shouldSkipIngestionWhenDisabled() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        when(props.isEnabled()).thenReturn(false);

        fxRateEnsurer.ensureFor(date);

        verify(props).isEnabled();
        verify(ingestion, never()).fetchAndUpsert(any(LocalDate.class));
    }

    @Test
    void ensureFor_shouldSkipIngestionWhenDateIsNull() {
        when(props.isEnabled()).thenReturn(true);

        fxRateEnsurer.ensureFor(null);

        verify(ingestion, never()).fetchAndUpsert(any(LocalDate.class));
    }

    @Test
    void ensureForRange_shouldTriggerRangeIngestionWhenEnabled() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(props.isEnabled()).thenReturn(true);

        fxRateEnsurer.ensureForRange(start, end);

        verify(props).isEnabled();
        verify(ingestion).fetchAndUpsert(start, end);
    }

    @Test
    void ensureForRange_shouldSkipIngestionWhenDisabled() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(props.isEnabled()).thenReturn(false);

        fxRateEnsurer.ensureForRange(start, end);

        verify(props).isEnabled();
        verify(ingestion, never()).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void ensureForRange_shouldSkipIngestionWhenStartDateIsNull() {
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(props.isEnabled()).thenReturn(true);

        fxRateEnsurer.ensureForRange(null, end);

        verify(ingestion, never()).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void ensureForRange_shouldSkipIngestionWhenEndDateIsNull() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        when(props.isEnabled()).thenReturn(true);

        fxRateEnsurer.ensureForRange(start, null);

        verify(ingestion, never()).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void ensureForRange_shouldSkipIngestionWhenBothDatesAreNull() {
        when(props.isEnabled()).thenReturn(true);

        fxRateEnsurer.ensureForRange(null, null);

        verify(ingestion, never()).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void ensureFor_shouldHandleMultipleCallsIndependently() {
        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LocalDate date2 = LocalDate.of(2024, 1, 2);
        when(props.isEnabled()).thenReturn(true);

        fxRateEnsurer.ensureFor(date1);
        fxRateEnsurer.ensureFor(date2);

        verify(ingestion).fetchAndUpsert(date1);
        verify(ingestion).fetchAndUpsert(date2);
        verify(ingestion, times(2)).fetchAndUpsert(any(LocalDate.class));
    }
}
