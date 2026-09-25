package com.finvault.common.repository;

import com.finvault.common.domain.ImportJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    List<ImportJob> findByUserIdOrderByCreatedAtDesc(Long userId);
}

