package com.navan.takehome.repository;

import com.navan.takehome.model.InvoiceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceTransactionRepository extends JpaRepository<InvoiceTransactionEntity, Long> {
    boolean existsByTransactionId(String transactionId);
}
