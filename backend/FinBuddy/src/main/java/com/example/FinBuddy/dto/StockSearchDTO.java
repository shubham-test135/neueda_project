package com.example.FinBuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for stock search results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSearchDTO {

    private String symbol;
    private String description; // Company name
    private String displaySymbol;
    private String type; // Common Stock, ETF, Bond, etc.
    private String currency;
    private String exchange;
    private String mic; // Market Identifier Code

    // Additional helper fields
    private Boolean inWishlist; // Check if already in user's wishlist
}