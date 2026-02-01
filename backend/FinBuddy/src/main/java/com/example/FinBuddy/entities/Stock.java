package com.example.FinBuddy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Stock entity representing equity investments
 */
@Entity
@DiscriminatorValue("STOCK")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Stock extends Asset {

    @Column(name = "exchange")
    private String exchange; // e.g., NYSE, NASDAQ, NSE

    @Column(name = "sector")
    private String sector; // e.g., Technology, Healthcare

    @Column(name = "market_cap", precision = 19, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "dividend_yield", precision = 10, scale = 4)
    private BigDecimal dividendYield = BigDecimal.ZERO;

    @Column(name = "pe_ratio", precision = 10, scale = 2)
    private BigDecimal peRatio;

    @Override
    public String getAssetType() {
        return "STOCK";
    }
}
