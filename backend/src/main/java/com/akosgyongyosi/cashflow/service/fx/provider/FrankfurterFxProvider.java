package com.akosgyongyosi.cashflow.service.fx.provider;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

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

    @SuppressWarnings("unchecked")
    @Override
    public Map<Currency, BigDecimal> getDailyQuotes(LocalDate date, Currency base, Set<Currency> quotes) {
        Map<Currency, BigDecimal> out = new EnumMap<>(Currency.class);
        if (quotes == null || quotes.isEmpty()) return out;

        String toParam = String.join(",", quotes.stream().filter(q -> q != base).map(Enum::name).toList());

        String url = UriComponentsBuilder
                .fromHttpUrl(props.getApiBaseUrl())
                .pathSegment(date.toString())                // /YYYY-MM-DD
                .queryParam("from", base.name())             // from=EUR
                .queryParam("to", toParam)                   // to=HUF,USD
                .build()
                .toUriString();

        // Try a few times in case the remote API is slow/unreliable. Use a short socket/read timeout
        int attempts = 3;
        int attempt = 0;
        Map<String, Object> body = null;
        while (attempt < attempts) {
            attempt++;
            try {
                // create a short-timeout RestTemplate for this outbound call so a slow provider won't hang us
                SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
                rf.setConnectTimeout(5_000); // 5s connect
                rf.setReadTimeout(5_000);    // 5s read
                RestTemplate local = new RestTemplate(rf);
                body = local.getForObject(url, Map.class);
                break;
            } catch (ResourceAccessException rae) {
                log.warn("Frankfurter API request attempt {}/{} failed for {}: {}", attempt, attempts, date, rae.getMessage());
                if (attempt >= attempts) {
                    log.error("Frankfurter API unreachable for {} after {} attempts, returning empty quotes", date, attempts);
                } else {
                    try { Thread.sleep(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }
            }
        }

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
                } catch (IllegalArgumentException ignore) { /* skip unknown code */ }
            }
        }
        return out;
    }
}
