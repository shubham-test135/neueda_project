package com.example.FinBuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for Dashboard summary data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private Long portfolioId;
    private String portfolioName;
    private BigDecimal totalValue;
    private BigDecimal totalInvestment;
    private BigDecimal totalGainLoss;
    private BigDecimal gainLossPercentage;
    private String baseCurrency;

    // Asset allocation breakdown
    private List<AssetAllocationDTO> assetAllocation;

    // Top performing assets
    private java.util.List<AssetPerformanceDTO> topPerformers;

    // Recent transactions count
    private Integer assetCount;
    private Integer wishlistCount;
}
