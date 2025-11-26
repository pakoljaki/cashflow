package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StartupFxIngestionRunnerTest {
    private FxIngestionService ingestion;
    private FxProperties props;
    private ExchangeRateCache cache;
    private ExchangeRateRepository repo;
    private StartupFxIngestionRunner runner;

    @BeforeEach
    void setup() {
        ingestion = mock(FxIngestionService.class);
        cache = mock(ExchangeRateCache.class);
        repo = mock(ExchangeRateRepository.class);
        props = new FxProperties();
        props.setStartupBackfillDays(10);
        props.setChunkSizeDays(4);
        runner = new StartupFxIngestionRunner(ingestion, props, cache, repo);
    }

    @Test
    void chunkedIngestionInvokesExpectedWindows() {
        runner.onReady();
    // LocalDate today = LocalDate.now(); // start variable not needed for assertions
        // Verify range calls sequence (we do not assert exact dates strongly; more granular matching possible)
        verify(ingestion, times(3)).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
        verify(cache).loadAll(10);
        InOrder order = inOrder(ingestion, cache);
        order.verify(ingestion, times(3)).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
        order.verify(cache).loadAll(10);
    }
}
