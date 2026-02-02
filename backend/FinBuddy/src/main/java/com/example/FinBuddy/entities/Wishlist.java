package com.example.FinBuddy.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a stock/security in user's wishlist
 * Tracks stocks user wants to monitor and potentially buy
 */
@Entity
@Table(name = "wishlist",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "symbol"})
        },
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_symbol", columnList = "symbol"),
                @Index(name = "idx_added_at", columnList = "added_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String symbol; // Stock ticker symbol (e.g., AAPL, GOOGL, TSLA)

    @Column(length = 200)
    private String name; // Company/Security name

    @Column(length = 50)
    private String exchange; // Exchange (e.g., NASDAQ, NYSE)

    @Column(length = 20)
    private String type; // Type: Stock, ETF, Bond, Mutual Fund

    @Column(length = 10)
    private String currency; // Currency code (e.g., USD, EUR)

    @Column(precision = 15, scale = 2)
    private BigDecimal targetPrice; // User's desired buy price

    @Column(precision = 15, scale = 2)
    private BigDecimal currentPrice; // Latest fetched price

    @Column(precision = 15, scale = 2)
    private BigDecimal previousClose; // Previous day's closing price

    @Column(precision = 15, scale = 2)
    private BigDecimal dayHigh; // Day's high price

    @Column(precision = 15, scale = 2)
    private BigDecimal dayLow; // Day's low price

    @Column(precision = 15, scale = 2)
    private BigDecimal openPrice; // Day's opening price

    @Column(length = 1000)
    private String notes; // User's personal notes

    @Column(nullable = false)
    private Boolean priceAlertEnabled = false; // Enable/disable price alerts

    @Column(nullable = false)
    private Boolean targetPriceMet = false; // Flag when target price is reached

    @Column(nullable = false)
    private Integer priority = 3; // Priority: 1 (High), 2 (Medium), 3 (Low)

    @Column(length = 50)
    private String sector; // Industry sector

    @Column(length = 100)
    private String industry; // Specific industry

    @Column(length = 50)
    private String marketCap; // Market capitalization category

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_price_update")
    private LocalDateTime lastPriceUpdate;

    @Column(name = "last_alert_sent")
    private LocalDateTime lastAlertSent; // Track when last alert was sent

    @Column(nullable = false)
    private Boolean active = true; // Soft delete flag

    /**
     * Calculate if target price is currently met
     */
    @Transient
    public boolean isTargetPriceCurrentlyMet() {
        if (targetPrice == null || currentPrice == null) {
            return false;
        }
        return currentPrice.compareTo(targetPrice) <= 0;
    }


    /**
     * Calculate price change from previous close
     */
    @Transient
    public BigDecimal getPriceChange() {
        if (currentPrice == null || previousClose == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(previousClose);
    }

    /**
     * Calculate price change percentage
     */
    @Transient
    public BigDecimal getPriceChangePercentage() {
        if (currentPrice == null || previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getPriceChange()
                .divide(previousClose, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Calculate distance to target price
     */
    @Transient
    public BigDecimal getDistanceToTarget() {
        if (targetPrice == null || currentPrice == null) {
            return null;
        }
        return currentPrice.subtract(targetPrice);
    }

    /**
     * Calculate percentage distance to target
     */
    @Transient
    public BigDecimal getDistanceToTargetPercentage() {
        if (targetPrice == null || currentPrice == null || targetPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return getDistanceToTarget()
                .divide(targetPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
        if (priority == null) {
            priority = 3;
        }
        if (active == null) {
            active = true;
        }
        if (priceAlertEnabled == null) {
            priceAlertEnabled = false;
        }
        if (targetPriceMet == null) {
            targetPriceMet = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Update targetPriceMet flag
        if (targetPrice != null && currentPrice != null) {
            targetPriceMet = currentPrice.compareTo(targetPrice) <= 0;
        }
    }
}