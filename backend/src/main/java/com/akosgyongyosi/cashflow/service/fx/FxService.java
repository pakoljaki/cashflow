package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.dto.FxWarningDTO;
import com.akosgyongyosi.cashflow.dto.RateMetaDTO;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class FxService {

    private final RateLookupService rateLookup;
    private static final Currency CANONICAL_BASE = Currency.EUR;
    private static final Logger log = LoggerFactory.getLogger(FxService.class);

    public FxService(RateLookupService rateLookup) {
        this.rateLookup = rateLookup;
    }

    /** Cross-rate: from→to = (BASE→to) / (BASE→from), evaluated at 'date' */
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        return convertWithDetails(amount, from, to, date).convertedAmount();
    }

    /**
     * Extended conversion returning metadata for both legs of the cross-rate and accumulated warnings.
     * Currently reuses legacy lookup logic; will be replaced by RateLookupService in a later step.
     */
    public ConversionResult convertWithDetails(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from == to) {
            log.debug("FX SAME {} -> {} @ {} amount={} (no conversion)", from, to, date, amount);
            return new ConversionResult(amount, new ArrayList<>(),
                    new RateMetaDTO(date, CANONICAL_BASE, to, BigDecimal.ONE, false, "N/A"),
                    new RateMetaDTO(date, CANONICAL_BASE, from, BigDecimal.ONE, false, "N/A"));
        }
        // Optimize & fix canonical base edge cases to avoid querying CANONICAL_BASE->CANONICAL_BASE which does not exist.
        List<FxWarningDTO> warnings = new ArrayList<>();
        if (from == CANONICAL_BASE) {
            var toRes = rateLookup.lookup(CANONICAL_BASE, to, date);
            warnings.addAll(toRes.getWarnings());
            // Direct conversion: from canonical base to target quote
            BigDecimal converted = amount.multiply(toRes.getRate()).setScale(8, RoundingMode.HALF_UP);
            log.debug("FX DIRECT {} (base) -> {} @ {} rate={} amount={} converted={}", from, to, date, toRes.getRate(), amount, converted);
            return new ConversionResult(converted, warnings, toRes.getMeta(), new RateMetaDTO(date, CANONICAL_BASE, from, BigDecimal.ONE, false, toRes.getMeta().getProvider()));
        }
        if (to == CANONICAL_BASE) {
            var fromRes = rateLookup.lookup(CANONICAL_BASE, from, date);
            warnings.addAll(fromRes.getWarnings());
            // Inverse conversion: from non-canonical into canonical base
            BigDecimal converted = amount.divide(fromRes.getRate(), 8, RoundingMode.HALF_UP);
            log.debug("FX INVERSE {} -> {} (base) @ {} rate={} amount={} converted={}", from, to, date, fromRes.getRate(), amount, converted);
            return new ConversionResult(converted, warnings, new RateMetaDTO(date, CANONICAL_BASE, to, BigDecimal.ONE, false, fromRes.getMeta().getProvider()), fromRes.getMeta());
        }
        // General cross-rate path: (BASE->to)/(BASE->from)
        var toRes = rateLookup.lookup(CANONICAL_BASE, to, date);
        var fromRes = rateLookup.lookup(CANONICAL_BASE, from, date);
        BigDecimal baseToTo = toRes.getRate();
        BigDecimal baseToFrom = fromRes.getRate();
        BigDecimal converted = amount.multiply(baseToTo).divide(baseToFrom, 8, RoundingMode.HALF_UP);
        warnings.addAll(toRes.getWarnings());
        warnings.addAll(fromRes.getWarnings());
        log.debug("FX CROSS {} -> {} via {} @ {} baseToTo={} baseToFrom={} amount={} converted={}", from, to, CANONICAL_BASE, date, baseToTo, baseToFrom, amount, converted);
        return new ConversionResult(converted, warnings, toRes.getMeta(), fromRes.getMeta());
    }

    public record ConversionResult(BigDecimal convertedAmount,
                                    List<FxWarningDTO> warnings,
                                    RateMetaDTO baseToQuoteMeta,
                                    RateMetaDTO baseToFromMeta) {}
}