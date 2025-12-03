package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.FxVolatilityDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FxAnalyticsService {

    private final ExchangeRateRepository repo;
    private final FxProperties props;

    public FxAnalyticsService(ExchangeRateRepository repo, FxProperties props) {
        this.repo = repo;
        this.props = props;
    }

    public List<FxVolatilityDTO> getVolatility(int days) {
        int window = days <= 0 ? 30 : days;
        LocalDate end = LocalDate.now().minusDays(1); 
    LocalDate start = end.minusDays(window - 1L);
        Currency base = props.getCanonicalBase();
        List<FxVolatilityDTO> out = new ArrayList<>();
        for (Currency q : props.getQuotes()) {
            if (q == base) continue;
            List<ExchangeRate> rates = repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(base, q, start, end);
            out.add(compute(base, q, window, rates));
        }
        return out;
    }

    private FxVolatilityDTO compute(Currency base, Currency quote, int window, List<ExchangeRate> rates) {
        int n = rates.size();
        if (n == 0) return new FxVolatilityDTO(base, quote, window, 0, null, null, null, null, true);
        BigDecimal sum = BigDecimal.ZERO;
    BigDecimal min = null;
    BigDecimal max = null;
        for (ExchangeRate er : rates) {
            BigDecimal r = er.getRateMid();
            sum = sum.add(r);
            min = (min == null || r.compareTo(min) < 0) ? r : min;
            max = (max == null || r.compareTo(max) > 0) ? r : max;
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP);
        if (n == 1) {
            return new FxVolatilityDTO(base, quote, window, 1, mean, BigDecimal.ZERO, min, max, window != 1);
        }
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (ExchangeRate er : rates) {
            BigDecimal diff = er.getRateMid().subtract(mean);
            BigDecimal sq = diff.multiply(diff);
            varianceSum = varianceSum.add(sq);
        }
    BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(n - 1L), 10, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(10, RoundingMode.HALF_UP);
        boolean partial = n < window;
        return new FxVolatilityDTO(base, quote, window, n, mean, stdDev, min, max, partial);
    }
}
