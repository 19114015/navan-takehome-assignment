package com.navan.takehome.repository;

import com.navan.takehome.model.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
    Optional<TransactionEntity> findByExternalId(String externalId);
}
