package com.finvault.common.repository;

import com.finvault.common.domain.SchedulerAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerAuditRepository extends JpaRepository<SchedulerAudit, Long> {
}

