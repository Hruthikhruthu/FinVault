package com.finvault.common.service;

import com.finvault.common.domain.AuditLog;
import com.finvault.common.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogger {
    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private final AuditLogRepository repository;

    public AuditLogger(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actor, String action, String target, String status, String details, String ip, String userAgent) {
        try {
            AuditLog entry = new AuditLog();
            entry.setActorEmail(actor);
            entry.setAction(action);
            entry.setTarget(target);
            entry.setStatus(status);
            entry.setDetails(details);
            entry.setIpAddress(ip);
            entry.setUserAgent(userAgent);
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Audit logging failed for action {}", action, ex);
        }
    }
}

