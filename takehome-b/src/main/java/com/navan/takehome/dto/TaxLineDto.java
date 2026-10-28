package com.navan.takehome.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TaxLineDto {
    @NotNull
    private String name;
    private BigDecimal rate;
    @NotNull
    private BigDecimal amount;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
