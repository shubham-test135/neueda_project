package com.example.FinBuddy.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for fetching real-time stock prices from Finnhub API
 * Free API: https://finnhub.io (60 calls/minute)
 * Alternative APIs: Alpha Vantage, Twelve Data, Yahoo Finance
 */
@Service
@Slf4j
public class StockPriceService {

    private final WebClient finnhubClient;
    private final WebClient alphaVantageClient;
    private final String apiKey;
    private final String alphaVantageKey;
    private final boolean apiEnabled;

    // Cache to reduce API calls (5-minute cache)
    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    public StockPriceService(
            WebClient.Builder webClientBuilder,
            @Value("${finnhub.api.key:demo}") String apiKey,
            @Value("${alphavantage.api.key:demo}") String alphaVantageKey,
            @Value("${finnhub.api.enabled:false}") boolean apiEnabled) {
        this.finnhubClient = webClientBuilder
                .baseUrl("https://finnhub.io/api/v1")
                .build();
        this.alphaVantageClient = webClientBuilder
                .baseUrl("https://www.alphavantage.co")
                .build();
        this.apiKey = apiKey;
        this.alphaVantageKey = alphaVantageKey;
        this.apiEnabled = apiEnabled;
        log.info("StockPriceService initialized - API enabled: {}, Finnhub key: {}, AlphaVantage key: {}",
                apiEnabled,
                apiKey.equals("demo") ? "demo" : "configured",
                alphaVantageKey.equals("demo") ? "demo" : "configured");
    }

    /**
     * Get real-time stock price from Finnhub or return cached/mock data
     */
    public BigDecimal getRealTimePrice(String symbol) {
        try {
            // Check cache first
            CachedPrice cached = priceCache.get(symbol);
            if (cached != null && !cached.isExpired()) {
                log.debug("Returning cached price for {}: {}", symbol, cached.price);
                return cached.price;
            }

            if (apiEnabled && !"demo".equals(apiKey)) {
                // Fetch from Finnhub API
                BigDecimal realPrice = fetchFromFinnhub(symbol);
                if (realPrice != null) {
                    priceCache.put(symbol, new CachedPrice(realPrice));
                    return realPrice;
                }
            }

            // Fallback to mock price
            BigDecimal mockPrice = getMockPrice(symbol);
            priceCache.put(symbol, new CachedPrice(mockPrice));
            return mockPrice;

        } catch (Exception e) {
            log.error("Error fetching stock price for {}: {}", symbol, e.getMessage());
            return getMockPrice(symbol);
        }
    }

    /**
     * Fetch price from Finnhub API
     */
    private BigDecimal fetchFromFinnhub(String symbol) {
        try {
            log.debug("Fetching price from Finnhub for: {}", symbol);

            Map<String, Object> response = finnhubClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbol.toUpperCase())
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .doOnError(WebClientResponseException.class, ex -> {
                        log.error("Finnhub API HTTP error for {}: {} - {}", symbol, ex.getStatusCode(),
                                ex.getResponseBodyAsString());
                    })
                    .onErrorResume(error -> {
                        log.warn("Finnhub API error for {}: {}", symbol, error.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response != null) {
                log.debug("Finnhub response for {}: {}", symbol, response);

                if (response.containsKey("c")) {
                    // "c" is current price in Finnhub response
                    Object priceObj = response.get("c");
                    double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0;

                    if (price > 0) {
                        log.info("✓ Fetched Finnhub price for {}: ${}", symbol, price);
                        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
                    } else {
                        log.warn("Finnhub returned zero price for {}, trying Alpha Vantage", symbol);
                    }
                } else {
                    log.warn("Finnhub response missing 'c' field for {}: {}", symbol, response);
                }
            }

            // Try Alpha Vantage as fallback
            return fetchFromAlphaVantage(symbol);

        } catch (Exception e) {
            log.error("Finnhub API call failed for {}: {}", symbol, e.getMessage());
            // Try Alpha Vantage as fallback
            return fetchFromAlphaVantage(symbol);
        }
    }

    /**
     * Fetch price from Alpha Vantage API as fallback
     */
    private BigDecimal fetchFromAlphaVantage(String symbol) {
        if ("demo".equals(alphaVantageKey)) {
            log.debug("Alpha Vantage key not configured, skipping");
            return null;
        }

        try {
            log.debug("Fetching price from Alpha Vantage for: {}", symbol);

            Map<String, Object> response = alphaVantageClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", symbol.toUpperCase())
                            .queryParam("apikey", alphaVantageKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(error -> {
                        log.warn("Alpha Vantage API error for {}: {}", symbol, error.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response != null && response.containsKey("Global Quote")) {
                Map<String, Object> quote = (Map<String, Object>) response.get("Global Quote");
                if (quote.containsKey("05. price")) {
                    String priceStr = quote.get("05. price").toString();
                    double price = Double.parseDouble(priceStr);

                    if (price > 0) {
                        log.info("✓ Fetched Alpha Vantage price for {}: ${}", symbol, price);
                        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }

            log.warn("Invalid Alpha Vantage response for {}", symbol);
            return null;

        } catch (Exception e) {
            log.error("Alpha Vantage API call failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Get multiple stock prices in batch
     */
    public Map<String, BigDecimal> getBatchPrices(java.util.List<String> symbols) {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (String symbol : symbols) {
            BigDecimal price = getRealTimePrice(symbol);
            if (price != null) {
                prices.put(symbol, price);
            }
        }
        return prices;
    }

    /**
     * Get batch detailed quotes with change information
     */
    public java.util.List<Map<String, Object>> getBatchDetailedQuotes(java.util.List<String> symbols) {
        java.util.List<Map<String, Object>> quotes = new java.util.ArrayList<>();
        for (String symbol : symbols) {
            Map<String, Object> quote = getDetailedQuote(symbol);
            // Rename fields to match frontend expectations
            Map<String, Object> formattedQuote = new HashMap<>();
            formattedQuote.put("symbol", symbol);
            formattedQuote.put("name", getCompanyName(symbol));
            formattedQuote.put("price", quote.get("currentPrice"));
            formattedQuote.put("change", quote.get("change"));
            formattedQuote.put("changePercent", quote.get("changePercent"));
            formattedQuote.put("timestamp", quote.get("timestamp"));
            quotes.add(formattedQuote);
        }
        return quotes;
    }

    /**
     * Get company name from symbol (mock implementation)
     */
    private String getCompanyName(String symbol) {
        // Map common symbols to company names
        Map<String, String> companyNames = new HashMap<>();
        companyNames.put("AAPL", "Apple Inc.");
        companyNames.put("GOOGL", "Alphabet Inc.");
        companyNames.put("MSFT", "Microsoft Corporation");
        companyNames.put("AMZN", "Amazon.com Inc.");
        companyNames.put("TSLA", "Tesla Inc.");
        companyNames.put("META", "Meta Platforms Inc.");
        companyNames.put("NVDA", "NVIDIA Corporation");
        companyNames.put("JPM", "JPMorgan Chase & Co.");
        companyNames.put("V", "Visa Inc.");
        companyNames.put("WMT", "Walmart Inc.");

        return companyNames.getOrDefault(symbol, symbol + " Inc.");
    }

    /**
     * Mock price generator for demo purposes
     * Generates realistic stock prices with daily variation
     */
    private BigDecimal getMockPrice(String symbol) {
        // Generate pseudo-random but consistent prices based on symbol hash
        int hash = Math.abs(symbol.hashCode());
        double basePrice = 50 + (hash % 450); // Price between $50-$500

        // Add some daily variation based on current time
        long dayFactor = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
        double variance = (Math.sin(dayFactor + hash) * 5); // ±$5 daily variance

        return BigDecimal.valueOf(basePrice + variance)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get exchange rate for currency conversion
     * For demo: returns mock rates
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // Mock exchange rates (for demo)
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("EUR", new BigDecimal("0.92"));
        usdRates.put("GBP", new BigDecimal("0.79"));
        usdRates.put("INR", new BigDecimal("83.12"));
        usdRates.put("JPY", new BigDecimal("149.50"));
        usdRates.put("CNY", new BigDecimal("7.24"));

        if (fromCurrency.equals("USD")) {
            return usdRates.getOrDefault(toCurrency, BigDecimal.ONE);
        } else if (toCurrency.equals("USD")) {
            BigDecimal rate = usdRates.get(fromCurrency);
            return rate != null ? BigDecimal.ONE.divide(rate, 4, RoundingMode.HALF_UP) : BigDecimal.ONE;
        }

        // For other currency pairs, convert via USD
        BigDecimal fromToUsd = getExchangeRate(fromCurrency, "USD");
        BigDecimal usdToTarget = getExchangeRate("USD", toCurrency);
        return fromToUsd.multiply(usdToTarget);
    }

    /**
     * Get benchmark index value (S&P 500, NIFTY 50, etc.)
     */
    public BigDecimal getBenchmarkValue(String indexSymbol) {
        Map<String, BigDecimal> mockIndexValues = new HashMap<>();
        mockIndexValues.put("^GSPC", new BigDecimal("4783.45")); // S&P 500
        mockIndexValues.put("^NSEI", new BigDecimal("21731.40")); // NIFTY 50
        mockIndexValues.put("^DJI", new BigDecimal("37305.16")); // Dow Jones
        mockIndexValues.put("^IXIC", new BigDecimal("14813.92")); // NASDAQ

        return mockIndexValues.getOrDefault(indexSymbol, BigDecimal.ZERO);
    }

    /**
     * Get benchmark index with change information
     */
    public Map<String, Object> getBenchmarkWithChange(String indexSymbol) {
        Map<String, Object> benchmark = new HashMap<>();

        BigDecimal currentValue = getBenchmarkValue(indexSymbol);
        // Mock change data - in production, this would come from the API
        BigDecimal previousClose = currentValue.multiply(BigDecimal.valueOf(0.995)); // Mock 0.5% change
        BigDecimal change = currentValue.subtract(previousClose);
        BigDecimal changePercent = change.divide(previousClose, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        benchmark.put("symbol", indexSymbol);
        benchmark.put("value", currentValue);
        benchmark.put("previousClose", previousClose);
        benchmark.put("change", change);
        benchmark.put("changePercent", changePercent);
        benchmark.put("timestamp", System.currentTimeMillis());

        return benchmark;
    }

    /**
     * Get detailed quote information including price change
     */
    public Map<String, Object> getDetailedQuote(String symbol) {
        Map<String, Object> quote = new HashMap<>();

        BigDecimal currentPrice = getRealTimePrice(symbol);
        BigDecimal previousClose = currentPrice.multiply(BigDecimal.valueOf(0.98)); // Mock 2% change
        BigDecimal change = currentPrice.subtract(previousClose);
        BigDecimal changePercent = change.divide(previousClose, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        quote.put("symbol", symbol);
        quote.put("currentPrice", currentPrice);
        quote.put("previousClose", previousClose);
        quote.put("change", change);
        quote.put("changePercent", changePercent);
        quote.put("timestamp", System.currentTimeMillis());

        return quote;
    }

    /**
     * Clear price cache (useful for manual refresh)
     */
    public void clearCache() {
        priceCache.clear();
        log.info("Price cache cleared");
    }

    /**
     * Search for stocks by symbol or name
     */
    public java.util.List<Map<String, Object>> searchStocks(String query) {
        java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();

        // Mock stock database - in production, this would query a real database or API
        String[][] stockDatabase = {
                { "AAPL", "Apple Inc.", "Technology", "NASDAQ" },
                { "GOOGL", "Alphabet Inc.", "Technology", "NASDAQ" },
                { "MSFT", "Microsoft Corporation", "Technology", "NASDAQ" },
                { "AMZN", "Amazon.com Inc.", "E-Commerce", "NASDAQ" },
                { "TSLA", "Tesla Inc.", "Automotive", "NASDAQ" },
                { "META", "Meta Platforms Inc.", "Technology", "NASDAQ" },
                { "NVDA", "NVIDIA Corporation", "Technology", "NASDAQ" },
                { "JPM", "JPMorgan Chase & Co.", "Banking", "NYSE" },
                { "V", "Visa Inc.", "Finance", "NYSE" },
                { "WMT", "Walmart Inc.", "Retail", "NYSE" },
                { "TCS", "Tata Consultancy Services", "IT Services", "NSE" },
                { "INFY", "Infosys Ltd.", "IT Services", "NSE" },
                { "RELIANCE", "Reliance Industries", "Conglomerate", "NSE" },
                { "HDFCBANK", "HDFC Bank", "Banking", "NSE" },
                { "ICICIBANK", "ICICI Bank", "Banking", "NSE" },
                { "ITC", "ITC Limited", "FMCG", "NSE" },
                { "SBIN", "State Bank of India", "Banking", "NSE" },
                { "BHARTIARTL", "Bharti Airtel", "Telecom", "NSE" },
                { "KOTAKBANK", "Kotak Mahindra Bank", "Banking", "NSE" },
                { "HINDUNILVR", "Hindustan Unilever", "FMCG", "NSE" }
        };

        String searchQuery = query.toLowerCase().trim();

        for (String[] stock : stockDatabase) {
            String symbol = stock[0];
            String name = stock[1];
            String sector = stock[2];
            String exchange = stock[3];

            // Search by symbol or name
            if (symbol.toLowerCase().contains(searchQuery) ||
                    name.toLowerCase().contains(searchQuery) ||
                    sector.toLowerCase().contains(searchQuery)) {

                BigDecimal price = getRealTimePrice(symbol);

                Map<String, Object> result = new HashMap<>();
                result.put("symbol", symbol);
                result.put("name", name);
                result.put("sector", sector);
                result.put("exchange", exchange);
                result.put("price", price);
                result.put("currency", exchange.equals("NSE") ? "INR" : "USD");

                results.add(result);

                // Limit to 10 results
                if (results.size() >= 10) {
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Inner class for price caching
     */
    private static class CachedPrice {
        private final BigDecimal price;
        private final long timestamp;

        public CachedPrice(BigDecimal price) {
            this.price = price;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
    public Map<String, Object> fetchStockData(String symbol) {

        Map<String, Object> data = new HashMap<>();

        BigDecimal currentPrice = getRealTimePrice(symbol);

        // Mock previous close (for demo)
        BigDecimal previousClose = currentPrice.multiply(BigDecimal.valueOf(0.98));
        BigDecimal changeAmount = currentPrice.subtract(previousClose);
        BigDecimal changePercentage = changeAmount
                .divide(previousClose, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        data.put("symbol", symbol);
        data.put("name", getCompanyName(symbol));
        data.put("currentPrice", currentPrice);
        data.put("changeAmount", changeAmount);
        data.put("changePercentage", changePercentage);

        // Optional fields (safe defaults)
        data.put("dayHigh", currentPrice.multiply(BigDecimal.valueOf(1.02)));
        data.put("dayLow", currentPrice.multiply(BigDecimal.valueOf(0.98)));
        data.put("marketCap", "N/A");

        return data;
    }

}
