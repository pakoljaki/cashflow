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

    public BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        return convertWithDetails(amount, from, to, date).convertedAmount();
    }

   
    public ConversionResult convertWithDetails(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from == to) {
            log.debug("FX SAME {} -> {} @ {} amount={} (no conversion)", from, to, date, amount);
            return new ConversionResult(amount, new ArrayList<>(),
                    new RateMetaDTO(date, CANONICAL_BASE, to, BigDecimal.ONE, false, "N/A"),
                    new RateMetaDTO(date, CANONICAL_BASE, from, BigDecimal.ONE, false, "N/A"));
        }
        List<FxWarningDTO> warnings = new ArrayList<>();
        if (from == CANONICAL_BASE) {
            var toRes = rateLookup.lookup(CANONICAL_BASE, to, date);
            warnings.addAll(toRes.getWarnings());
            BigDecimal converted = amount.multiply(toRes.getRate()).setScale(8, RoundingMode.HALF_UP);
            log.debug("FX DIRECT {} (base) -> {} @ {} rate={} amount={} converted={}", from, to, date, toRes.getRate(), amount, converted);
            return new ConversionResult(converted, warnings, toRes.getMeta(), new RateMetaDTO(date, CANONICAL_BASE, from, BigDecimal.ONE, false, toRes.getMeta().getProvider()));
        }
        if (to == CANONICAL_BASE) {
            var fromRes = rateLookup.lookup(CANONICAL_BASE, from, date);
            warnings.addAll(fromRes.getWarnings());
            BigDecimal converted = amount.divide(fromRes.getRate(), 8, RoundingMode.HALF_UP);
            log.debug("FX INVERSE {} -> {} (base) @ {} rate={} amount={} converted={}", from, to, date, fromRes.getRate(), amount, converted);
            return new ConversionResult(converted, warnings, new RateMetaDTO(date, CANONICAL_BASE, to, BigDecimal.ONE, false, fromRes.getMeta().getProvider()), fromRes.getMeta());
        }
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