package com.navan.takehome.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class InvoiceRequest {
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("transaction_ids")
    private List<String> transactionIds;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public List<String> getTransactionIds() { return transactionIds; }
    public void setTransactionIds(List<String> transactionIds) { this.transactionIds = transactionIds; }
}
