package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.dto.FxSettingsDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.service.fx.FxSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxSettingsAdminControllerTest {

    @Mock
    private FxSettingsService fxSettingsService;

    private FxSettingsAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new FxSettingsAdminController(fxSettingsService);
    }

    @Test
    void get_shouldReturnEffectiveSettings() {
        FxSettingsDTO expected = new FxSettingsDTO();
        expected.setEnabled(true);
        expected.setBaseCurrency(Currency.EUR);
        expected.setQuotes(List.of(Currency.USD, Currency.HUF));
        expected.setProvider("Frankfurter");

        when(fxSettingsService.getEffective()).thenReturn(expected);

        ResponseEntity<FxSettingsDTO> response = controller.get();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void put_shouldUpdateSettingsAndReturnUpdated() {
        FxSettingsDTO input = new FxSettingsDTO();
        input.setEnabled(false);
        input.setBaseCurrency(Currency.EUR);
        input.setQuotes(List.of(Currency.USD));
        input.setProvider("TestProvider");

        FxSettingsDTO updated = new FxSettingsDTO();
        updated.setEnabled(false);
        updated.setBaseCurrency(Currency.EUR);
        updated.setQuotes(List.of(Currency.USD));
        updated.setProvider("TestProvider");

        when(fxSettingsService.update(input)).thenReturn(updated);

        ResponseEntity<FxSettingsDTO> response = controller.put(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
    }

    @Test
    void put_shouldHandleEnablingFxIngestion() {
        FxSettingsDTO input = new FxSettingsDTO();
        input.setEnabled(true);
        input.setBaseCurrency(Currency.EUR);
        input.setQuotes(List.of(Currency.USD, Currency.HUF));
        input.setProvider("Frankfurter");

        when(fxSettingsService.update(input)).thenReturn(input);

        ResponseEntity<FxSettingsDTO> response = controller.put(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isEnabled()).isTrue();
    }
}
