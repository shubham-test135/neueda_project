package com.example.FinBuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for asset allocation by type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetAllocationDTO {
    private String assetType;
    private BigDecimal totalValue;
    private BigDecimal percentage;
    private Integer count;
}
