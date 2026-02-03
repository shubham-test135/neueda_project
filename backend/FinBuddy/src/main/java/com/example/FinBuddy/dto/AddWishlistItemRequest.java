package com.example.FinBuddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for adding items to wishlist
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddWishlistItemRequest {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Category is required")
    private String category; // STOCK, BOND, CRYPTO, ETF, MUTUAL_FUND

    private BigDecimal targetPrice;

    private String notes;
}