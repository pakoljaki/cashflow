package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.FxSettingsDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.service.AuditLogService;
import com.akosgyongyosi.cashflow.service.fx.FxSettingsService;
import com.akosgyongyosi.cashflow.service.fx.FxRefreshService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/fx")
@PreAuthorize("hasRole('ADMIN')")
public class FxAdminController {

    private final ExchangeRateRepository repo;
    private final FxSettingsService settingsService;
    private final AuditLogService auditLogService;
    private final FxProperties fxProperties;
    private final FxRefreshService fxRefreshService;

    public FxAdminController(ExchangeRateRepository repo, 
                            FxSettingsService settingsService, 
                            AuditLogService auditLogService,
                            FxProperties fxProperties,
                            FxRefreshService fxRefreshService) {
        this.repo = repo;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
        this.fxProperties = fxProperties;
        this.fxRefreshService = fxRefreshService;
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

    @GetMapping("/settings")
    public ResponseEntity<FxSettingsDTO> getSettings() {
        return ResponseEntity.ok(settingsService.getEffective());
    }

    @PutMapping("/settings")
    public ResponseEntity<FxSettingsDTO> updateSettings(@RequestBody FxSettingsDTO dto, Principal principal) {
        FxSettingsDTO updated = settingsService.update(dto);
        auditLogService.logAction(principal.getName(), "UPDATE_FX_SETTINGS", 
            Map.of("enabled", dto.isEnabled(),
                   "baseCurrency", dto.getBaseCurrency() != null ? dto.getBaseCurrency().name() : "null",
                   "provider", dto.getProvider() != null ? dto.getProvider() : "null"));
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> getMode() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dynamicFetchEnabled", fxProperties.isDynamicFetchEnabled());
        response.put("lastRefresh", null); 
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mode/toggle")
    public ResponseEntity<Map<String, Object>> toggleMode(Principal principal) {
        boolean newValue = !fxProperties.isDynamicFetchEnabled();
        fxProperties.setDynamicFetchEnabled(newValue);
        
        auditLogService.logAction(principal.getName(), "TOGGLE_FX_MODE", 
            Map.of("dynamicFetchEnabled", newValue));
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dynamicFetchEnabled", newValue);
        response.put("message", newValue ? "Dynamic fetch mode enabled" : "Cache-first mode enabled");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(Principal principal) {
        int days = fxProperties.getStartupBackfillDays();
        int refreshedCount = fxRefreshService.refreshExchangeRates(days);
        
        auditLogService.logAction(principal.getName(), "MANUAL_FX_REFRESH", 
            Map.of("days", days, "refreshedCount", refreshedCount));
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("days", days);
        response.put("refreshedCount", refreshedCount);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
