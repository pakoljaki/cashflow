package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.service.AuditLogService;
import com.akosgyongyosi.cashflow.service.fx.FxRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxAdminControllerModeTest {

    @Mock
    private FxProperties fxProperties;
    
    @Mock
    private FxRefreshService fxRefreshService;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private Principal principal;
    
    private FxAdminController controller;

    @BeforeEach
    void setup() {
        controller = new FxAdminController(null, null, auditLogService, fxProperties, fxRefreshService);
    }

    @Test
    void getMode_returnsCacheFirstByDefault() {
        when(fxProperties.isDynamicFetchEnabled()).thenReturn(false);
        
        ResponseEntity<Map<String, Object>> response = controller.getMode();
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("dynamicFetchEnabled"));
    }

    @Test
    void getMode_returnsDynamicFetchWhenEnabled() {
        when(fxProperties.isDynamicFetchEnabled()).thenReturn(true);
        
        ResponseEntity<Map<String, Object>> response = controller.getMode();
        
        assertEquals(200, response.getStatusCodeValue());
        assertTrue((Boolean) response.getBody().get("dynamicFetchEnabled"));
    }

    @Test
    void toggleMode_switchesFromCacheFirstToDynamic() {
        when(fxProperties.isDynamicFetchEnabled()).thenReturn(false);
        when(principal.getName()).thenReturn("admin");
        
        ResponseEntity<Map<String, Object>> response = controller.toggleMode(principal);
        
        verify(fxProperties).setDynamicFetchEnabled(true);
        verify(auditLogService).logAction(eq("admin"), eq("TOGGLE_FX_MODE"), any(Map.class));
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("dynamicFetchEnabled"));
    }

    @Test
    void toggleMode_switchesFromDynamicToCacheFirst() {
        when(fxProperties.isDynamicFetchEnabled()).thenReturn(true);
        when(principal.getName()).thenReturn("admin");
        
        ResponseEntity<Map<String, Object>> response = controller.toggleMode(principal);
        
        verify(fxProperties).setDynamicFetchEnabled(false);
        verify(auditLogService).logAction(eq("admin"), eq("TOGGLE_FX_MODE"), any(Map.class));
        assertEquals(200, response.getStatusCodeValue());
        assertFalse((Boolean) response.getBody().get("dynamicFetchEnabled"));
    }

    @Test
    void refresh_triggersRefreshServiceAndReturnsResult() {
        when(fxProperties.getStartupBackfillDays()).thenReturn(1000);
        when(fxRefreshService.refreshExchangeRates(1000)).thenReturn(15);
        when(principal.getName()).thenReturn("admin");
        
        ResponseEntity<Map<String, Object>> response = controller.refresh(principal);
        
        verify(fxRefreshService).refreshExchangeRates(1000);
        verify(auditLogService).logAction(eq("admin"), eq("MANUAL_FX_REFRESH"), any(Map.class));
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1000, response.getBody().get("days"));
        assertEquals(15, response.getBody().get("refreshedCount"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void refresh_logsCorrectAuditInformation() {
        when(fxProperties.getStartupBackfillDays()).thenReturn(500);
        when(fxRefreshService.refreshExchangeRates(500)).thenReturn(10);
        when(principal.getName()).thenReturn("admin");
        
        controller.refresh(principal);
        
        // Then
        verify(auditLogService).logAction(
            eq("admin"), 
            eq("MANUAL_FX_REFRESH"), 
            argThat(map -> 
                map.get("days").equals(500) && 
                map.get("refreshedCount").equals(10)
            )
        );
    }
}
