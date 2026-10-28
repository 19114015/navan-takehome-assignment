package com.navan.takehome.model;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_transactions", indexes = {
    @Index(name = "idx_inv_txn_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_inv_txn_invoice_id", columnList = "invoice_id")
})
public class InvoiceTransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InvoiceEntity getInvoice() { return invoice; }
    public void setInvoice(InvoiceEntity invoice) { this.invoice = invoice; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
