package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.dto.FxVolatilityDTO;
import com.akosgyongyosi.cashflow.service.fx.FxAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settings/fx/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class FxAnalyticsController {

    private final FxAnalyticsService service;

    public FxAnalyticsController(FxAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/volatility")
    public ResponseEntity<List<FxVolatilityDTO>> volatility(@RequestParam(name = "days", required = false) Integer days) {
        int d = days != null ? days : 30;
        return ResponseEntity.ok(service.getVolatility(d));
    }
}
