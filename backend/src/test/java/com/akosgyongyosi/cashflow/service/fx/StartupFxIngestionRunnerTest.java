package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class StartupFxIngestionRunnerTest {
    private FxRefreshService refreshService;
    private FxProperties props;
    private ExchangeRateRepository repo;
    private StartupFxIngestionRunner runner;

    @BeforeEach
    void setup() {
        refreshService = mock(FxRefreshService.class);
        repo = mock(ExchangeRateRepository.class);
        props = new FxProperties();
        props.setStartupBackfillDays(10);
        props.setChunkSizeDays(4);
        runner = new StartupFxIngestionRunner(refreshService, props, repo);
    }

    @Test
    void onReady_callsRefreshService() {
        when(repo.count()).thenReturn(0L);
        
        runner.onReady();
        
        verify(refreshService).refreshExchangeRates(10);
    }
    
    @Test
    void onReady_skipsRefreshWhenDataExists() {
        when(repo.count()).thenReturn(1000L);
        ExchangeRate latest = new ExchangeRate();
        latest.setRateDate(LocalDate.now().minusDays(1));
        when(repo.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(any(), any()))
            .thenReturn(Optional.of(latest));
        
        runner.onReady();
        
        verify(refreshService, never()).refreshExchangeRates(anyInt());
    }
}
