package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.FxSettingsDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.FxSettings;
import com.akosgyongyosi.cashflow.repository.FxSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FxSettingsServiceTest {

    @Mock
    private FxSettingsRepository repo;

    @Mock
    private FxProperties props;

    @InjectMocks
    private FxSettingsService fxSettingsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getEffective_shouldReturnDatabaseSettingsWhenExists() {
        FxSettings dbSettings = new FxSettings();
        dbSettings.setBaseCurrency(Currency.EUR);
        dbSettings.setApiBaseUrl("https://api.example.com");
        dbSettings.setQuotesCsv("USD,HUF");
        dbSettings.setRefreshCron("0 0 * * *");
        dbSettings.setEnabled(true);
        dbSettings.setProvider("CustomProvider");

        when(repo.findTopByOrderByIdAsc()).thenReturn(Optional.of(dbSettings));

        FxSettingsDTO result = fxSettingsService.getEffective();

        assertThat(result.getBaseCurrency()).isEqualTo(Currency.EUR);
        assertThat(result.getApiBaseUrl()).isEqualTo("https://api.example.com");
        assertThat(result.getQuotes()).containsExactly(Currency.USD, Currency.HUF);
        assertThat(result.getRefreshCron()).isEqualTo("0 0 * * *");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getProvider()).isEqualTo("CustomProvider");
        
        verify(repo).findTopByOrderByIdAsc();
    }

    @Test
    void getEffective_shouldReturnPropertiesDefaultsWhenNoDatabaseSettings() {
        when(repo.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(props.getCanonicalBase()).thenReturn(Currency.USD);
        when(props.getApiBaseUrl()).thenReturn("https://default.api.com");
        when(props.getQuotes()).thenReturn(Set.of(Currency.EUR, Currency.HUF));
        when(props.getRefreshCron()).thenReturn("0 0 12 * * *");
        when(props.isEnabled()).thenReturn(false);

        FxSettingsDTO result = fxSettingsService.getEffective();

        assertThat(result.getBaseCurrency()).isEqualTo(Currency.USD);
        assertThat(result.getApiBaseUrl()).isEqualTo("https://default.api.com");
        assertThat(result.getQuotes()).containsExactlyInAnyOrder(Currency.EUR, Currency.HUF);
        assertThat(result.getRefreshCron()).isEqualTo("0 0 12 * * *");
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getProvider()).isEqualTo("ECB/Frankfurter");
    }

    @Test
    void update_shouldCreateNewSettingsWhenNoneExist() {
        FxSettingsDTO input = new FxSettingsDTO();
        input.setBaseCurrency(Currency.EUR);
        input.setApiBaseUrl("https://new.api.com");
        input.setQuotes(List.of(Currency.USD, Currency.HUF));
        input.setRefreshCron("0 */6 * * *");
        input.setEnabled(true);
        input.setProvider("NewProvider");

        // Mock the second call for getEffective()
        FxSettings savedSettings = new FxSettings();
        savedSettings.setBaseCurrency(Currency.EUR);
        savedSettings.setApiBaseUrl("https://new.api.com");
        savedSettings.setQuotesCsv("USD,HUF");
        savedSettings.setRefreshCron("0 */6 * * *");
        savedSettings.setEnabled(true);
        savedSettings.setProvider("NewProvider");
        
        when(repo.findTopByOrderByIdAsc())
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(savedSettings));
        when(repo.save(any(FxSettings.class))).thenAnswer(i -> i.getArgument(0));

        FxSettingsDTO result = fxSettingsService.update(input);

        ArgumentCaptor<FxSettings> captor = ArgumentCaptor.forClass(FxSettings.class);
        verify(repo).save(captor.capture());
        
        FxSettings saved = captor.getValue();
        assertThat(saved.getBaseCurrency()).isEqualTo(Currency.EUR);
        assertThat(saved.getApiBaseUrl()).isEqualTo("https://new.api.com");
        assertThat(saved.getQuotesCsv()).isEqualTo("USD,HUF");
        assertThat(saved.isEnabled()).isTrue();
        
        verify(props).setCanonicalBase(Currency.EUR);
        verify(props).setApiBaseUrl("https://new.api.com");
        verify(props).setEnabled(true);
    }

    @Test
    void update_shouldUpdateExistingSettings() {
        FxSettings existingSettings = new FxSettings();
        existingSettings.setBaseCurrency(Currency.USD);
        
        FxSettingsDTO input = new FxSettingsDTO();
        input.setBaseCurrency(Currency.EUR);
        input.setApiBaseUrl("https://updated.api.com");
        input.setQuotes(List.of(Currency.USD));
        input.setRefreshCron("0 0 * * *");
        input.setEnabled(false);
        input.setProvider("UpdatedProvider");

        when(repo.findTopByOrderByIdAsc())
            .thenReturn(Optional.of(existingSettings))
            .thenReturn(Optional.of(existingSettings));
        when(repo.save(any(FxSettings.class))).thenAnswer(i -> i.getArgument(0));

        FxSettingsDTO result = fxSettingsService.update(input);

        ArgumentCaptor<FxSettings> captor = ArgumentCaptor.forClass(FxSettings.class);
        verify(repo).save(captor.capture());
        
        FxSettings saved = captor.getValue();
        assertThat(saved.getBaseCurrency()).isEqualTo(Currency.EUR);
        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    void update_shouldHandleEmptyQuotesList() {
        FxSettingsDTO input = new FxSettingsDTO();
        input.setBaseCurrency(Currency.USD);
        input.setApiBaseUrl("https://api.com");
        input.setQuotes(List.of()); // Empty list triggers EnumSet.copyOf exception
        input.setRefreshCron("0 0 * * *");
        input.setEnabled(true);

        when(repo.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fxSettingsService.update(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Collection is empty");
    }

    @Test
    void update_shouldHandleNullQuotesList() {
        FxSettingsDTO input = new FxSettingsDTO();
        input.setBaseCurrency(Currency.USD);
        input.setApiBaseUrl("https://api.com");
        input.setQuotes(null);
        input.setRefreshCron("0 0 * * *");
        input.setEnabled(true);

        FxSettings savedSettings = new FxSettings();
        savedSettings.setBaseCurrency(Currency.USD);
        savedSettings.setQuotesCsv("");
        
        when(repo.findTopByOrderByIdAsc())
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(savedSettings));
        when(repo.save(any(FxSettings.class))).thenAnswer(i -> i.getArgument(0));

        fxSettingsService.update(input);

        ArgumentCaptor<FxSettings> captor = ArgumentCaptor.forClass(FxSettings.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getQuotesCsv()).isEqualTo("");
    }

    @Test
    void update_shouldSetDefaultProviderWhenNull() {
        FxSettingsDTO input = new FxSettingsDTO();
        input.setBaseCurrency(Currency.USD);
        input.setApiBaseUrl("https://api.com");
        input.setQuotes(List.of(Currency.EUR));
        input.setRefreshCron("0 0 * * *");
        input.setEnabled(true);
        input.setProvider(null);

        FxSettings savedSettings = new FxSettings();
        savedSettings.setProvider("ECB/Frankfurter");
        
        when(repo.findTopByOrderByIdAsc())
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(savedSettings));
        when(repo.save(any(FxSettings.class))).thenAnswer(i -> i.getArgument(0));

        fxSettingsService.update(input);

        ArgumentCaptor<FxSettings> captor = ArgumentCaptor.forClass(FxSettings.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo("ECB/Frankfurter");
    }

    @Test
    void getEffective_shouldParseCsvCorrectly() {
        FxSettings dbSettings = new FxSettings();
        dbSettings.setBaseCurrency(Currency.USD);
        dbSettings.setQuotesCsv("EUR, HUF,  USD  ");
        dbSettings.setApiBaseUrl("https://api.com");
        dbSettings.setRefreshCron("0 0 * * *");
        dbSettings.setEnabled(true);

        when(repo.findTopByOrderByIdAsc()).thenReturn(Optional.of(dbSettings));

        FxSettingsDTO result = fxSettingsService.getEffective();

        assertThat(result.getQuotes()).containsExactly(Currency.EUR, Currency.HUF, Currency.USD);
    }

    @Test
    void getEffective_shouldHandleEmptyCsv() {
        FxSettings dbSettings = new FxSettings();
        dbSettings.setBaseCurrency(Currency.USD);
        dbSettings.setQuotesCsv("");
        dbSettings.setApiBaseUrl("https://api.com");
        dbSettings.setRefreshCron("0 0 * * *");
        dbSettings.setEnabled(true);

        when(repo.findTopByOrderByIdAsc()).thenReturn(Optional.of(dbSettings));

        FxSettingsDTO result = fxSettingsService.getEffective();

        assertThat(result.getQuotes()).isEmpty();
    }
}
