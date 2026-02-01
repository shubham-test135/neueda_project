package com.example.FinBuddy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Mutual Fund entity representing mutual fund investments
 */
@Entity
@DiscriminatorValue("MUTUAL_FUND")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MutualFund extends Asset {

    @Column(name = "nav", precision = 19, scale = 4)
    private BigDecimal nav; // Net Asset Value

    @Column(name = "fund_type")
    private String fundType; // e.g., Equity, Debt, Hybrid, Index

    @Column(name = "fund_house")
    private String fundHouse; // e.g., Vanguard, Fidelity, HDFC

    @Column(name = "aum", precision = 19, scale = 2)
    private BigDecimal aum; // Assets Under Management

    @Column(name = "expense_ratio", precision = 10, scale = 4)
    private BigDecimal expenseRatio;

    @Column(name = "risk_level")
    private String riskLevel; // LOW, MEDIUM, HIGH

    @Column(name = "scheme_code")
    private String schemeCode;

    @Column(name = "category")
    private String category; // Large Cap, Mid Cap, Small Cap, etc.

    @Override
    public String getAssetType() {
        return "MUTUAL_FUND";
    }
}
