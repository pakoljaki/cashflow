package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

class FxRateEnsurerTest {
    private FxIngestionService ingestion;
    private FxProperties props;
    private FxRateEnsurer ensurer;
    private LocalDate date;

    @BeforeEach
    void setup() {
        ingestion = mock(FxIngestionService.class);
        props = new FxProperties();
        ensurer = new FxRateEnsurer(ingestion, props);
        date = LocalDate.now().minusDays(2);
    }

    @Test
    void ensureForSkipsWhenDynamicDisabled() {
        props.setEnabled(true);
        props.setDynamicFetchEnabled(false);
        ensurer.ensureFor(date);
        verifyNoInteractions(ingestion);
    }

    @Test
    void ensureForCallsIngestionWhenDynamicEnabled() {
        props.setEnabled(true);
        props.setDynamicFetchEnabled(true);
        ensurer.ensureFor(date);
        verify(ingestion).fetchAndUpsert(date);
    }

    @Test
    void ensureForRangeSkipsWhenDynamicDisabled() {
        props.setEnabled(true); props.setDynamicFetchEnabled(false);
        ensurer.ensureForRange(date.minusDays(5), date);
        verifyNoInteractions(ingestion);
    }

    @Test
    void ensureForRangeCallsIngestionWhenDynamicEnabled() {
        props.setEnabled(true); props.setDynamicFetchEnabled(true);
        LocalDate start = date.minusDays(5); LocalDate end = date;
        ensurer.ensureForRange(start, end);
        verify(ingestion).fetchAndUpsert(start, end);
    }
}
