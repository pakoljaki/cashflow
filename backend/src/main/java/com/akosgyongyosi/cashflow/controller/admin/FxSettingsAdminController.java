package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.dto.FxSettingsDTO;
import com.akosgyongyosi.cashflow.service.fx.FxSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/fx")
@PreAuthorize("hasRole('ADMIN')")
public class FxSettingsAdminController {

    private final FxSettingsService service;

    public FxSettingsAdminController(FxSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<FxSettingsDTO> get() {
        return ResponseEntity.ok(service.getEffective());
    }

    @PutMapping
    public ResponseEntity<FxSettingsDTO> put(@RequestBody FxSettingsDTO dto) {
        return ResponseEntity.ok(service.update(dto));
    }
}
