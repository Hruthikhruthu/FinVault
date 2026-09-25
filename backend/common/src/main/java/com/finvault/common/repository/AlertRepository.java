package com.finvault.common.repository;

import com.finvault.common.domain.Alert;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Alert> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);
}

