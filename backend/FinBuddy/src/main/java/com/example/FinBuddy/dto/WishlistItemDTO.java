package com.example.FinBuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for wishlist item responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDTO {
    private Long id;
    private String symbol;
    private String name;
    private String category;
    private BigDecimal targetPrice;
    private BigDecimal priceWhenAdded;
    private BigDecimal currentPrice;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal yearHigh;
    private BigDecimal yearLow;
    private String marketCap;
    private BigDecimal changePercentage;
    private BigDecimal changeAmount;
    private BigDecimal performanceSinceAdded;
    private String notes;
    private Boolean alertEnabled;
    private Boolean alertTriggered;
    private LocalDateTime addedAt;
    private LocalDateTime lastUpdated;
    private String currency;
}