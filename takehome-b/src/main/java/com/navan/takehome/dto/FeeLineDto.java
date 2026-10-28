package com.navan.takehome.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class FeeLineDto {
    @NotNull
    private String name;
    @NotNull
    private BigDecimal amount;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
