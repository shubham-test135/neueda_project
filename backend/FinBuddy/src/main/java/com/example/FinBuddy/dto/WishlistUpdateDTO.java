package com.example.FinBuddy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating wishlist item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistUpdateDTO {

    @DecimalMin(value = "0.01", message = "Target price must be greater than 0")
    private BigDecimal targetPrice;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    private Boolean priceAlertEnabled;

    private Integer priority; // 1 (High), 2 (Medium), 3 (Low)
}