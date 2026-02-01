package com.example.FinBuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for individual asset performance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetPerformanceDTO {
    private Long assetId;
    private String name;
    private String symbol;
    private String assetType;
    private BigDecimal currentValue;
    private BigDecimal gainLoss;
    private BigDecimal gainLossPercentage;
}
