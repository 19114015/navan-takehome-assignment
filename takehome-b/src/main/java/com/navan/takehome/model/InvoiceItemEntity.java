package com.navan.takehome.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoice_items")
public class InvoiceItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "source_transaction_id", nullable = false)
    private String sourceTransactionId;

    @Column(nullable = false)
    private String type;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Lob
    @Column(name = "tax_lines_json", nullable = false)
    private String taxLinesJson;

    @Lob
    @Column(name = "fee_lines_json", nullable = false)
    private String feeLinesJson;

    @Lob
    @Column(name = "metadata_json", nullable = false)
    private String metadataJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InvoiceEntity getInvoice() { return invoice; }
    public void setInvoice(InvoiceEntity invoice) { this.invoice = invoice; }
    public String getSourceTransactionId() { return sourceTransactionId; }
    public void setSourceTransactionId(String sourceTransactionId) { this.sourceTransactionId = sourceTransactionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getTaxLinesJson() { return taxLinesJson; }
    public void setTaxLinesJson(String taxLinesJson) { this.taxLinesJson = taxLinesJson; }
    public String getFeeLinesJson() { return feeLinesJson; }
    public void setFeeLinesJson(String feeLinesJson) { this.feeLinesJson = feeLinesJson; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
