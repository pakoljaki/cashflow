package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.dto.*;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.config.FxProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


@Service
public class RateLookupService {

    private static final Logger log = LoggerFactory.getLogger(RateLookupService.class);

    private final ExchangeRateRepository repo;
    private final FxRateEnsurer ensurer;
    private final ExchangeRateCache fxCache; 
    private final FxProperties props;
    private final FxLookupAuditService auditService;

    public RateLookupService(ExchangeRateRepository repo,
                             FxRateEnsurer ensurer,
                             FxLookupAuditService auditService,
                             ExchangeRateCache fxCache,
                             FxProperties props) {
        this.repo = repo; this.ensurer = ensurer; this.auditService = auditService; this.fxCache = fxCache; this.props = props;
    }

    private final Map<String, RateLookupResultDTO> resultCache = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public RateLookupResultDTO lookup(Currency base, Currency quote, LocalDate effectiveDate) {
        String key = base + "|" + quote + "|" + effectiveDate;
        RateLookupResultDTO cached = resultCache.get(key);
        if (cached != null) {
            return cached;
        }

        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            cached = resultCache.get(key);
            if (cached != null) {
                return cached;
            }

            List<FxWarningDTO> warnings = new ArrayList<>();
            LocalDate today = LocalDate.now();
            LocalDate rateDate = resolveRateDate(effectiveDate, today, warnings);
            boolean provisional = isProvisional(effectiveDate, today);

            if (!props.isDynamicFetchEnabled()) {
                BigDecimal cachedRate = fxCache.getRate(rateDate, quote);
                RateMetaDTO meta = new RateMetaDTO(rateDate.isAfter(today) ? today : rateDate, base, quote, cachedRate, provisional, "CACHE");
                return storeAndAudit(key, base, quote, effectiveDate, warnings, meta);
            }

            ensureRateAvailable(rateDate, warnings);
            ExchangeRate exchangeRate = fetchRateEntity(base, quote, rateDate, warnings);
            return storeAndAudit(key, base, quote, effectiveDate, warnings,
                    new RateMetaDTO(exchangeRate.getRateDate(), base, quote, exchangeRate.getRateMid(), provisional, exchangeRate.getProvider()));
        } finally {
            lock.unlock();
        }
    }

    public void clearCache() { resultCache.clear(); }

    private void ensureRateAvailable(LocalDate rateDate, List<FxWarningDTO> warnings) {
        try {
            ensurer.ensureFor(rateDate);
        } catch (Exception e) {
            log.warn("On-demand ingestion failed for {}: {}", rateDate, e.getMessage());
            warnings.add(new FxWarningDTO(FxWarningCode.EXTERNAL_SERVICE_FAILURE,
                    "External FX provider unreachable; fallback logic applied.", "ERROR"));
        }
    }

    private ExchangeRate fetchRateEntity(Currency base, Currency quote, LocalDate rateDate, List<FxWarningDTO> warnings) {
        ExchangeRate exact = repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(rateDate, base, quote).orElse(null);
        if (exact != null) {
            return exact;
        }

        ExchangeRate nearest = repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(base, quote, rateDate).orElse(null);
        if (nearest != null) {
            if (!nearest.getRateDate().equals(rateDate)) {
                warnings.add(new FxWarningDTO(FxWarningCode.HISTORICAL_GAP_FALLBACK,
                        "Exact date " + rateDate + " unavailable; used " + nearest.getRateDate() + ".", "WARN"));
            }
            return nearest;
        }

        ExchangeRate latest = repo.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(base, quote).orElse(null);
        if (latest != null) {
            warnings.add(new FxWarningDTO(FxWarningCode.HISTORICAL_GAP_TODAY_FALLBACK,
                    "No historical rate near " + rateDate + "; using latest (" + latest.getRateDate() + ").", "WARN"));
            return latest;
        }

        throw new IllegalStateException("No FX rate available for " + base + "->" + quote);
    }

    private LocalDate resolveRateDate(LocalDate effectiveDate, LocalDate today, List<FxWarningDTO> warnings) {
        if (effectiveDate == null) {
            return today;
        }

        if (effectiveDate.isAfter(today)) {
            warnings.add(new FxWarningDTO(FxWarningCode.FUTURE_DATE_FALLBACK,
                    "Future booking date " + effectiveDate + " -> using today's rate.", "WARN"));
            warnings.add(new FxWarningDTO(FxWarningCode.PROVISIONAL_RATE,
                    "Rate provisional until actual historical quote becomes available.", "INFO"));
            return today;
        }

        return effectiveDate;
    }

    private boolean isProvisional(LocalDate effectiveDate, LocalDate today) {
        return effectiveDate != null && effectiveDate.isAfter(today);
    }

    private RateLookupResultDTO storeAndAudit(String key,
                                           Currency base,
                                           Currency quote,
                                           LocalDate effectiveDate,
                                           List<FxWarningDTO> warnings,
                                           RateMetaDTO meta) {
        RateLookupResultDTO result = new RateLookupResultDTO(meta, warnings);
        resultCache.put(key, result);
        audit(base, quote, effectiveDate, meta, warnings);
        return result;
    }

    private void audit(Currency base, Currency quote, LocalDate effectiveDate, RateMetaDTO meta, List<FxWarningDTO> warnings) {
        try {
            auditService.recordLookup(base, quote, effectiveDate, meta, warnings);
        } catch (Exception ex) {
            log.debug("Audit record failed: {}", ex.getMessage());
        }
    }
}
