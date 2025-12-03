package com.akosgyongyosi.cashflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

    public void logAction(String userId, String action, Map<String, Object> details) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("userId=").append(sanitize(userId))
                  .append(" action=").append(sanitize(action));
        
        if (details != null && !details.isEmpty()) {
            details.forEach((key, value) -> {
                logMessage.append(" ")
                          .append(sanitize(key))
                          .append("=")
                          .append(sanitize(String.valueOf(value)));
            });
        }
        
        AUDIT_LOGGER.info(logMessage.toString());
    }

   
    public void logAction(String userId, String action) {
        logAction(userId, action, null);
    }

  
    public void logFailedAction(String userId, String action, String reason) {
        AUDIT_LOGGER.warn("userId={} action={} status=FAILED reason={}", 
                         sanitize(userId), 
                         sanitize(action), 
                         sanitize(reason));
    }


    private String sanitize(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace("\n", " ").replace("\r", " ");
    }
}
