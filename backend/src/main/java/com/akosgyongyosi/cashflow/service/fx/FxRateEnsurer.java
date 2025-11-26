package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FxRateEnsurer {

    private final FxIngestionService ingestion;
    private final FxProperties props;

    public FxRateEnsurer(FxIngestionService ingestion, FxProperties props) {
        this.ingestion = ingestion;
        this.props = props;
    }

    public void ensureFor(LocalDate bookingDate) {
        if (!props.isEnabled() || bookingDate == null || !props.isDynamicFetchEnabled()) return; // skip runtime fetch when dynamic disabled
        ingestion.fetchAndUpsert(bookingDate);
    }

    public void ensureForRange(LocalDate startInclusive, LocalDate endInclusive) {
        if (!props.isEnabled() || startInclusive == null || endInclusive == null || !props.isDynamicFetchEnabled()) return; // skip runtime fetch when dynamic disabled
        ingestion.fetchAndUpsert(startInclusive, endInclusive);
    }
}
