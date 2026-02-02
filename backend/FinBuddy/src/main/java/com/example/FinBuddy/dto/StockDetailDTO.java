package com.example.FinBuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for detailed stock information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDetailDTO {

    private String symbol;
    private String name;
    private String exchange;
    private String currency;
    private String type;

    // Price information
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal change;
    private BigDecimal changePercent;
    private Long volume;

    // Additional info
    private String marketCap;
    private BigDecimal peRatio;
    private String sector;
    private String industry;
    private String country;
    private String logo;
    private String weburl;

    // Helper fields
    private Boolean inWishlist;
    private Long wishlistId;
}