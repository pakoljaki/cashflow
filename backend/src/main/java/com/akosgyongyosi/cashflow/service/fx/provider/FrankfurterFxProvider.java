package com.akosgyongyosi.cashflow.service.fx.provider;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class FrankfurterFxProvider implements FxProvider {

    private final RestTemplate restTemplate;
    private final FxProperties props;

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

        Map<String, Object> body = restTemplate.getForObject(url, Map.class);
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
