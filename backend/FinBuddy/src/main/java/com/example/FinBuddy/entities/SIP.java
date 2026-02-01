package com.example.FinBuddy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SIP (Systematic Investment Plan) entity
 */
@Entity
@DiscriminatorValue("SIP")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SIP extends Asset {

    @Column(name = "monthly_investment", precision = 19, scale = 2)
    private BigDecimal monthlyInvestment;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "frequency")
    private String frequency = "MONTHLY"; // MONTHLY, QUARTERLY, YEARLY

    @Column(name = "scheme_name")
    private String schemeName;

    @Column(name = "fund_house")
    private String fundHouse; // e.g., HDFC, ICICI, SBI

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "total_installments")
    private Integer totalInstallments = 0;

    @Override
    public String getAssetType() {
        return "SIP";
    }
}
