package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.scheduler.FxScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

class ScheduledServicesGuardTest {
    private FxProperties props;
    private FxIngestionService ingestion;
    private FxRateEnsurer rateEnsurer;
    private RateLookupService rateLookup;
    private ForwardFxWarmupService warmupService;
    private ExchangeRateBackfillService backfillService;
    private FxScheduler scheduler;

    @BeforeEach
    void setup() {
        props = new FxProperties();
        ingestion = mock(FxIngestionService.class);
        rateEnsurer = mock(FxRateEnsurer.class);
        rateLookup = mock(RateLookupService.class);
        warmupService = new ForwardFxWarmupService(rateLookup, props);
        backfillService = new ExchangeRateBackfillService(rateEnsurer, null, props); // repo unused in guarded path
        scheduler = new FxScheduler(ingestion, props);
    }

    @Test
    void servicesSkipWhenDynamicDisabled() {
        props.setEnabled(true); props.setDynamicFetchEnabled(false);
        warmupService.warmForwardWindow();
        backfillService.dailyBackfill();
        scheduler.daily();
        verifyNoInteractions(rateEnsurer, ingestion, rateLookup);
    }

    @Test
    void schedulerRunsWhenDynamicEnabled() {
        props.setEnabled(true); props.setDynamicFetchEnabled(true);
        scheduler.daily();
        verify(ingestion).fetchAndUpsert(LocalDate.now());
    }
}
