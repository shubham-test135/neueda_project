package com.example.FinBuddy.controllers;

import com.example.FinBuddy.dto.WishlistCreateDTO;
import com.example.FinBuddy.dto.WishlistDTO;
import com.example.FinBuddy.dto.WishlistUpdateDTO;
import com.example.FinBuddy.services.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Wishlist Management
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist Management", description = "APIs for managing user's stock wishlist")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    @Operation(summary = "Add stock to wishlist",
            description = "Add a stock/security to user's wishlist with optional target price and alerts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Stock added to wishlist successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Stock already in wishlist")
    })
    public ResponseEntity<WishlistDTO> addToWishlist(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody WishlistCreateDTO createDTO) {
        WishlistDTO created = wishlistService.addToWishlist(userId, createDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get user's wishlist",
            description = "Retrieve all stocks in user's wishlist ordered by date added")
    @ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getUserWishlist(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<WishlistDTO> wishlist = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/by-priority")
    @Operation(summary = "Get wishlist by priority",
            description = "Retrieve wishlist items sorted by priority (High to Low)")
    @ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getWishlistByPriority(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<WishlistDTO> wishlist = wishlistService.getUserWishlistByPriority(userId);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/high-priority")
    @Operation(summary = "Get high priority items",
            description = "Retrieve only high priority wishlist items")
    @ApiResponse(responseCode = "200", description = "High priority items retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getHighPriorityItems(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<WishlistDTO> items = wishlistService.getHighPriorityItems(userId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{wishlistId}")
    @Operation(summary = "Get wishlist item",
            description = "Get details of a specific wishlist item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wishlist item retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    public ResponseEntity<WishlistDTO> getWishlistItem(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Wishlist Item ID") @PathVariable Long wishlistId) {
        WishlistDTO item = wishlistService.getWishlistItem(userId, wishlistId);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{wishlistId}")
    @Operation(summary = "Update wishlist item",
            description = "Update target price, notes, priority, or alert settings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wishlist item updated successfully"),
            @ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    public ResponseEntity<WishlistDTO> updateWishlistItem(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Wishlist Item ID") @PathVariable Long wishlistId,
            @Valid @RequestBody WishlistUpdateDTO updateDTO) {
        WishlistDTO updated = wishlistService.updateWishlistItem(userId, wishlistId, updateDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{wishlistId}")
    @Operation(summary = "Remove from wishlist",
            description = "Remove a stock from user's wishlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Stock removed successfully"),
            @ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    public ResponseEntity<Void> removeFromWishlist(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Wishlist Item ID") @PathVariable Long wishlistId) {
        wishlistService.removeFromWishlist(userId, wishlistId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{wishlistId}/refresh-price")
    @Operation(summary = "Refresh item price",
            description = "Refresh current price for a specific wishlist item")
    @ApiResponse(responseCode = "200", description = "Price refreshed successfully")
    public ResponseEntity<WishlistDTO> refreshItemPrice(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Wishlist Item ID") @PathVariable Long wishlistId) {
        WishlistDTO updated = wishlistService.refreshItemPrice(userId, wishlistId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/refresh-prices")
    @Operation(summary = "Refresh all prices",
            description = "Refresh current prices for all wishlist items")
    @ApiResponse(responseCode = "200", description = "Prices refreshed successfully")
    public ResponseEntity<Map<String, String>> refreshAllPrices(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        wishlistService.refreshAllPrices(userId);
        return ResponseEntity.ok(Map.of("message", "Prices refresh initiated successfully"));
    }

    @GetMapping("/target-met")
    @Operation(summary = "Get target price met items",
            description = "Get stocks where current price meets or is below target price")
    @ApiResponse(responseCode = "200", description = "Target met items retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getTargetPriceMetItems(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<WishlistDTO> items = wishlistService.getTargetPriceMetItems(userId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/alerts-enabled")
    @Operation(summary = "Get alert enabled items",
            description = "Get all items with price alerts enabled")
    @ApiResponse(responseCode = "200", description = "Alert enabled items retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getPriceAlertEnabledItems(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<WishlistDTO> items = wishlistService.getPriceAlertEnabledItems(userId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/by-type/{type}")
    @Operation(summary = "Get items by type",
            description = "Filter wishlist items by security type (Stock, ETF, Bond, etc.)")
    @ApiResponse(responseCode = "200", description = "Items retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getItemsByType(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Security Type (Stock, ETF, Bond)") @PathVariable String type) {
        List<WishlistDTO> items = wishlistService.getItemsByType(userId, type);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/by-sector/{sector}")
    @Operation(summary = "Get items by sector",
            description = "Filter wishlist items by industry sector")
    @ApiResponse(responseCode = "200", description = "Items retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getItemsBySector(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Industry Sector") @PathVariable String sector) {
        List<WishlistDTO> items = wishlistService.getItemsBySector(userId, sector);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/search")
    @Operation(summary = "Search wishlist",
            description = "Search wishlist by symbol or company name")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> searchWishlist(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Search term") @RequestParam String query) {
        List<WishlistDTO> items = wishlistService.searchWishlist(userId, query);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/top-performers")
    @Operation(summary = "Get top performers",
            description = "Get top performing stocks in wishlist based on price change")
    @ApiResponse(responseCode = "200", description = "Top performers retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getTopPerformers(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Number of items to return") @RequestParam(defaultValue = "5") int limit) {
        List<WishlistDTO> items = wishlistService.getTopPerformers(userId, limit);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/worst-performers")
    @Operation(summary = "Get worst performers",
            description = "Get worst performing stocks in wishlist based on price change")
    @ApiResponse(responseCode = "200", description = "Worst performers retrieved successfully")
    public ResponseEntity<List<WishlistDTO>> getWorstPerformers(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Number of items to return") @RequestParam(defaultValue = "5") int limit) {
        List<WishlistDTO> items = wishlistService.getWorstPerformers(userId, limit);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get wishlist statistics",
            description = "Get comprehensive statistics about user's wishlist")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getWishlistStatistics(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        Map<String, Object> stats = wishlistService.getWishlistStatistics(userId);
        return ResponseEntity.ok(stats);
    }
}