package com.example.FinBuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Portfolio Performance Data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDataDTO {
    private List<String> dates;
    private List<BigDecimal> values;
}
