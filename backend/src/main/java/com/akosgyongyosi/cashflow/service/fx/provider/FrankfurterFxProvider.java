package com.akosgyongyosi.cashflow.service.fx.provider;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class FrankfurterFxProvider implements FxProvider {

    private final RestTemplate restTemplate;
    private final FxProperties props;
    private static final Logger log = LoggerFactory.getLogger(FrankfurterFxProvider.class);

    public FrankfurterFxProvider(RestTemplate restTemplate, FxProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    @Override
    public String getProviderName() {
        return "Frankfurter";
    }

    @Override
    public Map<Currency, BigDecimal> getDailyQuotes(LocalDate date, Currency base, Set<Currency> quotes) {
        Map<Currency, BigDecimal> out = new EnumMap<>(Currency.class);
        if (quotes == null || quotes.isEmpty()) return out;
        String toParam = String.join(",", quotes.stream().filter(q -> q != base).map(Enum::name).toList());
        String url = props.getApiBaseUrl() + "/" + date + "?from=" + base.name() + "&to=" + toParam;
        Map<String, Object> body = fetchJson(url, 1, 5_000, 5_000, date.toString());
        if (body == null) return out;
        Object ratesObj = body.get("rates");
        if (ratesObj instanceof Map<?, ?> rates) {
            for (Map.Entry<?, ?> e : rates.entrySet()) {
                String k = String.valueOf(e.getKey());
                Object v = e.getValue();
                try {
                    Currency c = Currency.valueOf(k);
                    BigDecimal val = (v instanceof Number) ? new BigDecimal(v.toString()) : null;
                    if (val != null) out.put(c, val);
                } catch (IllegalArgumentException ignore) {}
            }
        }
        return out;
    }

    @Override
    public Map<LocalDate, Map<Currency, BigDecimal>> getRangeQuotes(LocalDate start, LocalDate end, Currency base, Set<Currency> quotes) {
        if (start == null || end == null || end.isBefore(start) || quotes == null || quotes.isEmpty()) return Map.of();
        String toParam = String.join(",", quotes.stream().filter(q -> q != base).map(Enum::name).toList());
        String url = props.getApiBaseUrl() + "/?start=" + start + "&end=" + end + "&from=" + base.name() + "&to=" + toParam;
        Map<String, Object> body = fetchJson(url, 2, 5_000, 10_000, start + ".." + end);
        return parseRangeRates(body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchJson(String url, int attempts, int connectTimeoutMs, int readTimeoutMs, String context) {
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
                rf.setConnectTimeout(connectTimeoutMs);
                rf.setReadTimeout(readTimeoutMs);
                restTemplate.setRequestFactory(rf);
                Map<String, Object> body = restTemplate.getForObject(url, Map.class);
                return body != null ? body : Map.of();
            } catch (ResourceAccessException rae) {
                log.warn("Frankfurter API attempt {}/{} failed for {}: {}", attempt, attempts, context, rae.getMessage());
                if (attempt >= attempts) {
                    log.error("Frankfurter API unreachable for {} after {} attempts", context, attempts);
                    return Map.of();
                }
                try { Thread.sleep(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }
        return Map.of();
    }

    private Map<LocalDate, Map<Currency, BigDecimal>> parseRangeRates(Map<String, Object> body) {
        if (body == null || body.isEmpty()) return Map.of();
        Object ratesContainer = body.get("rates");
        if (!(ratesContainer instanceof Map<?,?> rawDates)) return Map.of();
        Map<LocalDate, Map<Currency, BigDecimal>> result = new LinkedHashMap<>();
        for (Map.Entry<?,?> dateEntry : rawDates.entrySet()) {
            LocalDate d = parseDateKey(dateEntry.getKey());
            if (d == null) continue;
            Map<Currency, BigDecimal> dayRates = parseDayRates(dateEntry.getValue());
            result.put(d, dayRates);
        }
        return result;
    }

    private LocalDate parseDateKey(Object key) {
        try { return LocalDate.parse(String.valueOf(key)); } catch (Exception ex) { return null; }
    }

    private Map<Currency, BigDecimal> parseDayRates(Object inner) {
        Map<Currency, BigDecimal> dayRates = new EnumMap<>(Currency.class);
        if (!(inner instanceof Map<?,?> innerMap)) return dayRates;
        for (Map.Entry<?,?> e : innerMap.entrySet()) {
            try {
                Currency c = Currency.valueOf(String.valueOf(e.getKey()));
                Object v = e.getValue();
                BigDecimal bd = (v instanceof Number) ? new BigDecimal(v.toString()) : null;
                if (bd != null) dayRates.put(c, bd);
            } catch (IllegalArgumentException ignore) { }
        }
        return dayRates;
    }
}
