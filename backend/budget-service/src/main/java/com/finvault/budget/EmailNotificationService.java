package com.finvault.budget;

import com.finvault.common.service.AuditLogger;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
    private final AuditLogger auditLogger;

    public EmailNotificationService(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void sendBudgetAlert(String userEmail, String subject, String body) {
        auditLogger.log("system", "EMAIL_NOTIFICATION", userEmail, "QUEUED", subject + " :: " + body, null, null);
    }
}

