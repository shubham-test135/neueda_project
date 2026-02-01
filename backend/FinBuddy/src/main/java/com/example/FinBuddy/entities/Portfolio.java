package com.example.FinBuddy.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Portfolio entity representing a user's investment portfolio
 */
@Entity
@Table(name = "portfolios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String baseCurrency = "USD"; // Default base currency

    @Column(nullable = false)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalInvestment = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalGainLoss = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal gainLossPercentage = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Asset> assets = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Recalculates portfolio metrics based on assets
     */
    public void recalculateMetrics() {
        this.totalValue = assets.stream()
                .map(Asset::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalInvestment = assets.stream()
                .map(Asset::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalGainLoss = this.totalValue.subtract(this.totalInvestment);

        if (this.totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            this.gainLossPercentage = this.totalGainLoss
                    .divide(this.totalInvestment, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        } else {
            this.gainLossPercentage = BigDecimal.ZERO;
        }
    }
}
