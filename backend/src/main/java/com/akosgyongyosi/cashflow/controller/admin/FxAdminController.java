package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.service.fx.FxIngestionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/fx")
public class FxAdminController {

    private final FxIngestionService ingestion;
    private final ExchangeRateRepository repo;

    public FxAdminController(FxIngestionService ingestion, ExchangeRateRepository repo) {
        this.ingestion = ingestion;
        this.repo = repo;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        var summary = ingestion.fetchAndUpsert(start, end);
        Map<String, Object> body = new HashMap<>();
        body.put("start", summary.getStart());
        body.put("end", summary.getEnd());
        body.put("inserted", summary.getTotalInserted());
        body.put("updated", summary.getTotalUpdated());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Currency c : Currency.values()) {
            repo.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(Currency.EUR, c)
                    .ifPresent(er -> out.put(c.name(), er.getRateDate()));
        }
        return ResponseEntity.ok(out);
    }
}
