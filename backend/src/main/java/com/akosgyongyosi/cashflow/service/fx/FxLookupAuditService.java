package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.dto.FxWarningDTO;
import com.akosgyongyosi.cashflow.dto.RateMetaDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.FxLookupAudit;
import com.akosgyongyosi.cashflow.repository.FxLookupAuditRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
public class FxLookupAuditService {

    private final FxLookupAuditRepository repo;

    public FxLookupAuditService(FxLookupAuditRepository repo) { this.repo = repo; }

    public void recordLookup(Currency base, Currency quote, LocalDate requestedDate, RateMetaDTO meta, List<FxWarningDTO> warnings) {
        FxLookupAudit a = new FxLookupAudit();
        a.setBaseCurrency(base);
        a.setQuoteCurrency(quote);
        a.setRequestedDate(requestedDate);
        a.setEffectiveRateDate(meta.getRateDateUsed());
        a.setRateMid(meta.getRate());
        a.setProvisional(meta.isProvisional());
        if (warnings != null && !warnings.isEmpty()) {
            a.setWarningCodes(warnings.stream().map(w -> w.getCode().name()).collect(Collectors.joining(",")));
        }
        repo.save(a);
    }
}
