package com.akosgyongyosi.cashflow.scheduler;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.service.fx.FxIngestionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class FxScheduler {

    private final FxIngestionService ingestion;
    private final FxProperties props;

    public FxScheduler(FxIngestionService ingestion, FxProperties props) {
        this.ingestion = ingestion;
        this.props = props;
    }

    @Scheduled(cron = "#{@fxProperties.refreshCron}")
    public void daily() {
        if (!props.isEnabled() || !props.isDynamicFetchEnabled()) return; // disable daily runtime ingestion in cache-only mode
        ingestion.fetchAndUpsert(LocalDate.now());
    }
}
