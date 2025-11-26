package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Pre-caches provisional future FX rates for next N days by invoking RateLookupService.
 * No DB writes for future dates occur (provider cannot supply future quotes); caching improves latency for planning screens.
 */
@Component
public class ForwardFxWarmupService {

    private static final Logger log = LoggerFactory.getLogger(ForwardFxWarmupService.class);
    private final RateLookupService rateLookupService;
    private final FxProperties props;

    public ForwardFxWarmupService(RateLookupService rateLookupService, FxProperties props) {
        this.rateLookupService = rateLookupService;
        this.props = props;
    }

    // Run daily at 04:05 local time.
    @Scheduled(cron = "0 5 4 * * ?")
    public void warmForwardWindow() {
        if (!props.isEnabled() || !props.isDynamicFetchEnabled()) return; // skip in cache-only mode
        int days = props.getForwardWarmDays();
        LocalDate today = LocalDate.now();
        for (Currency q : props.getQuotes()) {
            if (q == props.getCanonicalBase()) continue;
            for (int i = 1; i <= days; i++) {
                LocalDate future = today.plusDays(i);
                try { rateLookupService.lookup(props.getCanonicalBase(), q, future); } catch (Exception ex) {
                    log.debug("Forward warm lookup failed for {}->{} {}: {}", props.getCanonicalBase(), q, future, ex.getMessage());
                }
            }
        }
        log.info("Forward FX warm-up completed for {} days ahead (base {} quotes {} )", days, props.getCanonicalBase(), props.getQuotes());
    }
}
