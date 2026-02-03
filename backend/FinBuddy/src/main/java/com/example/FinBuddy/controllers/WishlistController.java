package com.example.FinBuddy.controllers;

import com.example.FinBuddy.dto.AddWishlistItemRequest;
import com.example.FinBuddy.dto.WishlistItemDTO;
import com.example.FinBuddy.dto.WishlistSummaryDTO;
import com.example.FinBuddy.services.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Wishlist operations (Portfolio-based)
 */
@RestController
@RequestMapping("/api/portfolios/{portfolioId}/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // tighten in production
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * Add item to wishlist
     * POST /api/portfolios/{portfolioId}/wishlist
     */
    @PostMapping
    public ResponseEntity<?> addToWishlist(
            @PathVariable Long portfolioId,
            @Valid @RequestBody AddWishlistItemRequest request
    ) {
        WishlistItemDTO item = wishlistService.addToWishlist(portfolioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Added to wishlist successfully",
                "data", item
        ));
    }

    /**
     * Get wishlist items
     * GET /api/portfolios/{portfolioId}/wishlist
     */
    @GetMapping
    public ResponseEntity<?> getWishlist(@PathVariable Long portfolioId) {
        List<WishlistItemDTO> items = wishlistService.getWishlist(portfolioId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", items
        ));
    }

    /**
     * Update wishlist item
     * PUT /api/portfolios/{portfolioId}/wishlist/{itemId}
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateWishlistItem(
            @PathVariable Long portfolioId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> updates
    ) {
        BigDecimal targetPrice = updates.containsKey("targetPrice")
                ? new BigDecimal(updates.get("targetPrice").toString())
                : null;

        String notes = (String) updates.get("notes");

        WishlistItemDTO item =
                wishlistService.updateWishlistItem(portfolioId, itemId, targetPrice, notes);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Wishlist item updated",
                "data", item
        ));
    }

    /**
     * Delete wishlist item
     * DELETE /api/portfolios/{portfolioId}/wishlist/{itemId}
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> deleteWishlistItem(
            @PathVariable Long portfolioId,
            @PathVariable Long itemId
    ) {
        wishlistService.removeFromWishlist(portfolioId, itemId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Removed from wishlist successfully"
        ));
    }

    /**
     * Refresh prices for wishlist
     * POST /api/portfolios/{portfolioId}/wishlist/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshPrices(@PathVariable Long portfolioId) {
        List<WishlistItemDTO> items = wishlistService.refreshPrices(portfolioId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Prices refreshed successfully",
                "data", items
        ));
    }

    /**
     * Wishlist summary
     * GET /api/portfolios/{portfolioId}/wishlist/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getWishlistSummary(@PathVariable Long portfolioId) {
        WishlistSummaryDTO summary = wishlistService.getWishlistSummary(portfolioId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", summary
        ));
    }
}
