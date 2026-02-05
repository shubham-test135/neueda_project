package com.example.FinBuddy.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Wishlist belongs to a Portfolio (NOT User)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonBackReference
    private Portfolio portfolio;

    @Column(nullable = false)
    private String symbol; // AAPL, INFY, TCS

    @Column(nullable = false)
    private String name; // Apple Inc.

    @Column(nullable = false)
    private String category; // STOCK, ETF, MF, BOND

    @Column(name = "price_when_added", precision = 19, scale = 4)
    private BigDecimal priceWhenAdded;

    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "target_price", precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Column(name = "change_percentage", precision = 10, scale = 4)
    private BigDecimal changePercentage;

    @Column(name = "change_amount", precision = 19, scale = 4)
    private BigDecimal changeAmount;

    @Column(name = "alert_enabled")
    private Boolean alertEnabled = false;

    @Column(name = "alert_triggered")
    private Boolean alertTriggered = false;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Column(nullable = false)
    private String currency = "USD";

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Performance since added (%)
     */
    public BigDecimal getPerformanceSinceAdded() {
        if (priceWhenAdded == null || currentPrice == null ||
                priceWhenAdded.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentPrice.subtract(priceWhenAdded)
                .divide(priceWhenAdded, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Alert trigger condition
     */
    public boolean shouldTriggerAlert() {
        return Boolean.TRUE.equals(alertEnabled)
                && !Boolean.TRUE.equals(alertTriggered)
                && targetPrice != null
                && currentPrice != null
                && currentPrice.compareTo(targetPrice) >= 0;
    }
}
