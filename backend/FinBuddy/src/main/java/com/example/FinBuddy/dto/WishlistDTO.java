package com.example.FinBuddy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Wishlist data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistDTO {

    private Long id;
    private Long userId;
    private String symbol;
    private String name;
    private String exchange;
    private String type;
    private String currency;

    // Price information
    private BigDecimal targetPrice;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal openPrice;

    // Calculated fields
    private BigDecimal priceChange;
    private BigDecimal priceChangePercentage;
    private BigDecimal distanceToTarget;
    private BigDecimal distanceToTargetPercentage;

    // User preferences
    private String notes;
    private Boolean priceAlertEnabled;
    private Boolean targetPriceMet;
    private Integer priority;

    // Additional information
    private String sector;
    private String industry;
    private String marketCap;

    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime addedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPriceUpdate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAlertSent;

    private Boolean active;

    // Helper field for UI
    private String priceStatus; // "Above Target", "At Target", "Below Target"
    private String priorityLabel; // "High", "Medium", "Low"
}