package com.akosgyongyosi.cashflow.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;


@SpringBootTest
class AuditLogServiceTest {

    @Autowired
    private AuditLogService auditLogService;

    @Test
    void testBasicAuditLog() {
        auditLogService.logAction("test@example.com", "TEST_ACTION");
        
        auditLogService.logAction("test@example.com", "TEST_ACTION_WITH_DETAILS", 
            Map.of("key1", "value1", "key2", 123));
        
        auditLogService.logFailedAction("test@example.com", "TEST_FAILED_ACTION", 
            "REASON_CODE");
        
        System.out.println("✓ Audit log entries written. Check backend/logs/audit.log");
    }
}
