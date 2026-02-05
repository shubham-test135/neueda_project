package com.example.FinBuddy.controllers;

import com.example.FinBuddy.entities.*;
import com.example.FinBuddy.services.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for Asset management
 */
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssetController {

    private final AssetService assetService;

    /**
     * Get all assets
     */
    @GetMapping
    public ResponseEntity<List<Asset>> getAllAssets() {
        List<Asset> assets = assetService.getAllAssets();
        return ResponseEntity.ok(assets);
    }

    /**
     * Get asset by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Asset> getAssetById(@PathVariable Long id) {
        return assetService.getAssetById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get assets by portfolio
     */
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<Asset>> getAssetsByPortfolio(@PathVariable Long portfolioId) {
        List<Asset> assets = assetService.getAssetsByPortfolio(portfolioId);
        return ResponseEntity.ok(assets);
    }

    /**
     * Get invested assets (non-wishlist)
     */
    @GetMapping("/portfolio/{portfolioId}/invested")
    public ResponseEntity<List<Asset>> getInvestedAssets(@PathVariable Long portfolioId) {
        List<Asset> assets = assetService.getInvestedAssets(portfolioId);
        return ResponseEntity.ok(assets);
    }

    /**
     * Get wishlist assets
     */
    @GetMapping("/portfolio/{portfolioId}/wishlist")
    public ResponseEntity<List<Asset>> getWishlistAssets(@PathVariable Long portfolioId) {
        List<Asset> assets = assetService.getWishlistAssets(portfolioId);
        return ResponseEntity.ok(assets);
    }

    /**
     * Search assets by name or symbol
     * Searches across all asset types: Stocks, Bonds, Mutual Funds, and SIPs
     */
    @GetMapping("/search")
    public ResponseEntity<List<Asset>> searchAssets(@RequestParam String query) {
        List<Asset> assets = assetService.searchAssets(query);
        return ResponseEntity.ok(assets);
    }

    /**
     * Search stocks only
     */
    @GetMapping("/search/stocks")
    public ResponseEntity<List<Asset>> searchStocks(@RequestParam String query) {
        List<Asset> stocks = assetService.searchStocks(query);
        return ResponseEntity.ok(stocks);
    }

    /**
     * Search bonds only
     */
    @GetMapping("/search/bonds")
    public ResponseEntity<List<Asset>> searchBonds(@RequestParam String query) {
        List<Asset> bonds = assetService.searchBonds(query);
        return ResponseEntity.ok(bonds);
    }

    /**
     * Search mutual funds only
     */
    @GetMapping("/search/mutualfunds")
    public ResponseEntity<List<Asset>> searchMutualFunds(@RequestParam String query) {
        List<Asset> mutualFunds = assetService.searchMutualFunds(query);
        return ResponseEntity.ok(mutualFunds);
    }

    /**
     * Search SIPs only
     */
    @GetMapping("/search/sips")
    public ResponseEntity<List<Asset>> searchSIPs(@RequestParam String query) {
        List<Asset> sips = assetService.searchSIPs(query);
        return ResponseEntity.ok(sips);
    }

    /**
     * Create new stock asset
     */
    @PostMapping("/stocks/{portfolioId}")
    public ResponseEntity<Asset> createStock(
            @PathVariable Long portfolioId,
            @RequestBody Stock stock) {
        Asset created = assetService.createAsset(stock, portfolioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Create new bond asset
     */
    @PostMapping("/bonds/{portfolioId}")
    public ResponseEntity<Asset> createBond(
            @PathVariable Long portfolioId,
            @RequestBody Bond bond) {
        Asset created = assetService.createAsset(bond, portfolioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Create new mutual fund asset
     */
    @PostMapping("/mutualfunds/{portfolioId}")
    public ResponseEntity<Asset> createMutualFund(
            @PathVariable Long portfolioId,
            @RequestBody MutualFund mutualFund) {
        Asset created = assetService.createAsset(mutualFund, portfolioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Create new SIP asset
     */
    @PostMapping("/sips/{portfolioId}")
    public ResponseEntity<Asset> createSIP(
            @PathVariable Long portfolioId,
            @RequestBody SIP sip) {
        Asset created = assetService.createAsset(sip, portfolioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update asset
     */
    @PutMapping("/{id}")
    public ResponseEntity<Asset> updateAsset(
            @PathVariable Long id,
            @RequestBody Asset asset) {
        try {
            Asset updated = assetService.updateAsset(id, asset);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update asset price (for real-time price updates)
     */
    @PatchMapping("/{id}/price")
    public ResponseEntity<Asset> updateAssetPrice(
            @PathVariable Long id,
            @RequestParam BigDecimal newPrice) {
        try {
            Asset updated = assetService.updateAssetPrice(id, newPrice);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete asset
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        try {
            assetService.deleteAsset(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get stocks only
     */
    @GetMapping("/stocks")
    public ResponseEntity<List<Asset>> getStocks() {
        List<Asset> stocks = assetService.getAssetsByType(Stock.class);
        return ResponseEntity.ok(stocks);
    }

    /**
     * Get bonds only
     */
    @GetMapping("/bonds")
    public ResponseEntity<List<Asset>> getBonds() {
        List<Asset> bonds = assetService.getAssetsByType(Bond.class);
        return ResponseEntity.ok(bonds);
    }

    /**
     * Get mutual funds only
     */
    @GetMapping("/mutualfunds")
    public ResponseEntity<List<Asset>> getMutualFunds() {
        List<Asset> funds = assetService.getAssetsByType(MutualFund.class);
        return ResponseEntity.ok(funds);
    }

    /**
     * Get SIPs only
     */
    @GetMapping("/sips")
    public ResponseEntity<List<Asset>> getSIPs() {
        List<Asset> sips = assetService.getAssetsByType(SIP.class);
        return ResponseEntity.ok(sips);
    }
}
