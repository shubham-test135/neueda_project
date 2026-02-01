package com.example.FinBuddy.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Base Asset entity representing different types of investments
 * Uses single table inheritance strategy for different asset types
 */
@Entity
@Table(name = "assets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "asset_type", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchasePrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal investedAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentValue;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal gainLoss = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal gainLossPercentage = BigDecimal.ZERO;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean isWishlist = false; // For sandboxing/wishlist feature

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonBackReference
    private Portfolio portfolio;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate current value and gain/loss metrics
     */
    public void calculateMetrics() {
        this.investedAmount = this.purchasePrice.multiply(new BigDecimal(this.quantity));
        this.currentValue = this.currentPrice.multiply(new BigDecimal(this.quantity));
        this.gainLoss = this.currentValue.subtract(this.investedAmount);

        if (this.investedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.gainLossPercentage = this.gainLoss
                    .divide(this.investedAmount, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        } else {
            this.gainLossPercentage = BigDecimal.ZERO;
        }
    }

    /**
     * Get the discriminator value (asset type)
     */
    public abstract String getAssetType();
}
