package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.FxSettingsDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.FxSettings;
import com.akosgyongyosi.cashflow.repository.FxSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FxSettingsService {

    private final FxSettingsRepository repo;
    private final FxProperties props;

    public FxSettingsService(FxSettingsRepository repo, FxProperties props) {
        this.repo = repo;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public FxSettingsDTO getEffective() {
        FxSettings s = repo.findTopByOrderByIdAsc().orElse(null);
        FxSettingsDTO dto = new FxSettingsDTO();
        if (s == null) {
            dto.setBaseCurrency(props.getCanonicalBase());
            dto.setApiBaseUrl(props.getApiBaseUrl());
            dto.setQuotes(List.copyOf(props.getQuotes()));
            dto.setRefreshCron(props.getRefreshCron());
            dto.setEnabled(props.isEnabled());
            dto.setProvider("ECB/Frankfurter");
            return dto;
        }
        dto.setBaseCurrency(s.getBaseCurrency());
        dto.setApiBaseUrl(s.getApiBaseUrl());
        dto.setQuotes(parseCsv(s.getQuotesCsv()));
        dto.setRefreshCron(s.getRefreshCron());
        dto.setEnabled(s.isEnabled());
        dto.setProvider(s.getProvider());
        return dto;
    }

    @Transactional
    public FxSettingsDTO update(FxSettingsDTO in) {
        FxSettings s = repo.findTopByOrderByIdAsc().orElseGet(FxSettings::new);
        s.setBaseCurrency(in.getBaseCurrency());
        s.setApiBaseUrl(in.getApiBaseUrl());
        s.setQuotesCsv(joinCsv(in.getQuotes()));
        s.setRefreshCron(in.getRefreshCron());
        s.setEnabled(in.isEnabled());
        s.setProvider(in.getProvider() != null ? in.getProvider() : "ECB/Frankfurter");
        repo.save(s);

        props.setCanonicalBase(in.getBaseCurrency());
        props.setApiBaseUrl(in.getApiBaseUrl());
        Set<Currency> set = in.getQuotes() == null ? EnumSet.noneOf(Currency.class) : EnumSet.copyOf(in.getQuotes());
        props.setQuotes(set);
        props.setEnabled(in.isEnabled());
        props.setRefreshCron(in.getRefreshCron());

        return getEffective();
    }

    private static List<Currency> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return List.of(csv.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Currency::valueOf)
                .collect(Collectors.toList());
    }

    private static String joinCsv(List<Currency> list) {
        if (list == null || list.isEmpty()) return "";
        return list.stream().map(Enum::name).collect(Collectors.joining(","));
    }
}
