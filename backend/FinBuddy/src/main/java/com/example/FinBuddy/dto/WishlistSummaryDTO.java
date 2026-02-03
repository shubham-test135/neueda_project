package com.example.FinBuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for wishlist summary statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistSummaryDTO {
    private Long totalWatchlist;
    private Long gainersCount;
    private Long losersCount;
    private Long alertsCount;
}