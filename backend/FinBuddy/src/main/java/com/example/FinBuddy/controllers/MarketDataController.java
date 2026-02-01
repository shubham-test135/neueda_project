package com.example.FinBuddy.controllers;

import com.example.FinBuddy.services.StockPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Stock Market Data
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final StockPriceService stockPriceService;

    /**
     * Get real-time stock price
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<Map<String, Object>> getStockPrice(@PathVariable String symbol) {
        try {
            BigDecimal price = stockPriceService.getRealTimePrice(symbol);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                return ResponseEntity.ok(Map.of(
                        "symbol", symbol,
                        "price", price,
                        "timestamp", System.currentTimeMillis(),
                        "source", "live"));
            }
            return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "price", BigDecimal.ZERO,
                    "timestamp", System.currentTimeMillis(),
                    "source", "unavailable",
                    "message", "Price data not available for symbol: " + symbol));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "symbol", symbol,
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * Get batch stock prices
     */
    @PostMapping("/prices/batch")
    public ResponseEntity<Map<String, BigDecimal>> getBatchPrices(@RequestBody List<String> symbols) {
        Map<String, BigDecimal> prices = stockPriceService.getBatchPrices(symbols);
        return ResponseEntity.ok(prices);
    }

    /**
     * Get exchange rate
     */
    @GetMapping("/exchange-rate")
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to) {
        BigDecimal rate = stockPriceService.getExchangeRate(from, to);
        return ResponseEntity.ok(Map.of(
                "from", from,
                "to", to,
                "rate", rate));
    }

    /**
     * Get benchmark index value
     */
    @GetMapping("/benchmark/{symbol}")
    public ResponseEntity<Map<String, Object>> getBenchmarkValue(@PathVariable String symbol) {
        BigDecimal value = stockPriceService.getBenchmarkValue(symbol);
        return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "value", value,
                "timestamp", System.currentTimeMillis()));
    }

    /**
     * Get detailed quote with price change information
     */
    @GetMapping("/quote/{symbol}")
    public ResponseEntity<Map<String, Object>> getDetailedQuote(@PathVariable String symbol) {
        Map<String, Object> quote = stockPriceService.getDetailedQuote(symbol);
        return ResponseEntity.ok(quote);
    }

    /**
     * Clear price cache - useful for forcing fresh data fetch
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        stockPriceService.clearCache();
        return ResponseEntity.ok(Map.of("message", "Price cache cleared successfully"));
    }
}
