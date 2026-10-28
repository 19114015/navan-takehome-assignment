package com.navan.takehome.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TransactionRequest {
    @NotNull
    private String type;

    @NotNull
    @JsonProperty("external_id")
    private String externalId;

    @NotNull
    private String currency;

    @NotNull
    @JsonProperty("occurred_at")
    private Instant occurredAt;

    @NotNull
    private BigDecimal total;

    @JsonProperty("tax_lines")
    private List<TaxLineDto> taxLines;

    @JsonProperty("fee_lines")
    private List<FeeLineDto> feeLines;

    @NotNull
    private Map<String, Object> metadata;

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
    public List<TaxLineDto> getTaxLines() { return taxLines; }
    public void setTaxLines(List<TaxLineDto> taxLines) { this.taxLines = taxLines; }
    public List<FeeLineDto> getFeeLines() { return feeLines; }
    public void setFeeLines(List<FeeLineDto> feeLines) { this.feeLines = feeLines; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
