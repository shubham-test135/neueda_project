package com.example.FinBuddy.controllers;

import com.example.FinBuddy.dto.StockDetailDTO;
import com.example.FinBuddy.dto.StockSearchDTO;
import com.example.FinBuddy.services.StockMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Stock Search and Market Data
 */
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock Search", description = "Search and retrieve live stock, bond, and ETF data")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StockSearchController {

    private final StockMarketService stockMarketService;

    @GetMapping("/search")
    @Operation(summary = "Search securities",
            description = "Search for stocks, bonds, ETFs, and other securities by symbol or name")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    public ResponseEntity<List<StockSearchDTO>> searchSecurities(
            @Parameter(description = "Search query (symbol or company name)", example = "Apple")
            @RequestParam String query) {
        List<StockSearchDTO> results = stockMarketService.searchSecurities(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/stocks")
    @Operation(summary = "Search US stocks",
            description = "Search specifically for US stocks on major exchanges")
    @ApiResponse(responseCode = "200", description = "US stock search results retrieved successfully")
    public ResponseEntity<List<StockSearchDTO>> searchUSStocks(
            @Parameter(description = "Search query", example = "Microsoft")
            @RequestParam String query) {
        List<StockSearchDTO> results = stockMarketService.searchUSStocks(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/bonds")
    @Operation(summary = "Search bonds",
            description = "Search for bonds and treasury securities")
    @ApiResponse(responseCode = "200", description = "Bond search results retrieved successfully")
    public ResponseEntity<List<StockSearchDTO>> searchBonds(
            @Parameter(description = "Search query", example = "treasury")
            @RequestParam String query) {
        List<StockSearchDTO> results = stockMarketService.searchBonds(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/etfs")
    @Operation(summary = "Search ETFs",
            description = "Search for Exchange Traded Funds")
    @ApiResponse(responseCode = "200", description = "ETF search results retrieved successfully")
    public ResponseEntity<List<StockSearchDTO>> searchETFs(
            @Parameter(description = "Search query", example = "SPY")
            @RequestParam String query) {
        List<StockSearchDTO> results = stockMarketService.searchETFs(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/mutual-funds")
    @Operation(summary = "Search Mutual Funds",
            description = "Search for mutual funds")
    @ApiResponse(responseCode = "200", description = "Mutual fund search results retrieved successfully")
    public ResponseEntity<List<StockSearchDTO>> searchMutualFunds(
            @Parameter(description = "Search query", example = "Vanguard")
            @RequestParam String query) {
        List<StockSearchDTO> results = stockMarketService.searchMutualFunds(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{symbol}")
    @Operation(summary = "Get stock details",
            description = "Get comprehensive information about a specific stock including price, profile, and market data")
    @ApiResponse(responseCode = "200", description = "Stock details retrieved successfully")
    public ResponseEntity<StockDetailDTO> getStockDetails(
            @Parameter(description = "Stock symbol", example = "AAPL")
            @PathVariable String symbol) {
        StockDetailDTO details = stockMarketService.getStockDetails(symbol.toUpperCase());
        return ResponseEntity.ok(details);
    }

    @GetMapping("/{symbol}/quote")
    @Operation(summary = "Get stock quote",
            description = "Get real-time price quote for a stock")
    @ApiResponse(responseCode = "200", description = "Quote retrieved successfully")
    public ResponseEntity<Map<String, Object>> getStockQuote(
            @Parameter(description = "Stock symbol", example = "AAPL")
            @PathVariable String symbol) {
        Map<String, Object> quote = stockMarketService.getStockQuote(symbol.toUpperCase());
        return ResponseEntity.ok(quote);
    }

    @GetMapping("/{symbol}/profile")
    @Operation(summary = "Get company profile",
            description = "Get company profile and information including sector, industry, market cap, etc.")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    public ResponseEntity<Map<String, Object>> getCompanyProfile(
            @Parameter(description = "Stock symbol", example = "AAPL")
            @PathVariable String symbol) {
        Map<String, Object> profile = stockMarketService.getCompanyProfile(symbol.toUpperCase());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/batch-quotes")
    @Operation(summary = "Get batch quotes",
            description = "Get quotes for multiple stocks in a single request")
    @ApiResponse(responseCode = "200", description = "Batch quotes retrieved successfully")
    public ResponseEntity<Map<String, Map<String, Object>>> getBatchQuotes(
            @Parameter(description = "List of stock symbols")
            @RequestBody List<String> symbols) {
        Map<String, Map<String, Object>> quotes = stockMarketService.getBatchQuotes(symbols);
        return ResponseEntity.ok(quotes);
    }
}