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
        // Map friendly names to actual ticker symbols
        Map<String, String> symbolMapping = new HashMap<>();
        symbolMapping.put("SP500", "^GSPC");
        symbolMapping.put("NIFTY50", "^NSEI");
        symbolMapping.put("DJI", "^DJI");
        symbolMapping.put("NASDAQ", "^IXIC");

        // Convert friendly name to ticker symbol if needed
        String actualSymbol = symbolMapping.getOrDefault(indexSymbol, indexSymbol);

        try {
            // Check cache first
            CachedPrice cached = priceCache.get(actualSymbol);
            if (cached != null && !cached.isExpired()) {
                log.debug("Returning cached index value for {}: {}", actualSymbol, cached.price);
                return cached.price;
            }

            if (apiEnabled && !"demo".equals(apiKey)) {
                // Fetch from Finnhub API
                BigDecimal realValue = fetchFromFinnhub(actualSymbol);
                if (realValue != null && realValue.compareTo(BigDecimal.ZERO) > 0) {
                    priceCache.put(actualSymbol, new CachedPrice(realValue));
                    return realValue;
                }
            }

            // Fallback to mock values if API is disabled or fails
            log.debug("Using fallback mock data for index: {}", actualSymbol);
            Map<String, BigDecimal> mockIndexValues = new HashMap<>();
            mockIndexValues.put("^GSPC", new BigDecimal("4783.45")); // S&P 500
            mockIndexValues.put("^NSEI", new BigDecimal("21731.40")); // NIFTY 50
            mockIndexValues.put("^DJI", new BigDecimal("37305.16")); // Dow Jones
            mockIndexValues.put("^IXIC", new BigDecimal("14813.92")); // NASDAQ

            BigDecimal fallbackValue = mockIndexValues.getOrDefault(actualSymbol, BigDecimal.ZERO);
            if (fallbackValue.compareTo(BigDecimal.ZERO) > 0) {
                priceCache.put(actualSymbol, new CachedPrice(fallbackValue));
            }
            return fallbackValue;

        } catch (Exception e) {
            log.error("Error fetching index value for {}: {}", actualSymbol, e.getMessage());
            // Return fallback value
            Map<String, BigDecimal> mockIndexValues = new HashMap<>();
            mockIndexValues.put("^GSPC", new BigDecimal("4783.45"));
            mockIndexValues.put("^NSEI", new BigDecimal("21731.40"));
            mockIndexValues.put("^DJI", new BigDecimal("37305.16"));
            mockIndexValues.put("^IXIC", new BigDecimal("14813.92"));
            return mockIndexValues.getOrDefault(actualSymbol, BigDecimal.ZERO);
        }
    }

    /**
     * Get benchmark index with change information
     */
    public Map<String, Object> getBenchmarkWithChange(String indexSymbol) {
        Map<String, Object> benchmark = new HashMap<>();

        // Map friendly names to actual ticker symbols
        Map<String, String> symbolMapping = new HashMap<>();
        symbolMapping.put("SP500", "^GSPC");
        symbolMapping.put("NIFTY50", "^NSEI");
        symbolMapping.put("DJI", "^DJI");
        symbolMapping.put("NASDAQ", "^IXIC");

        String actualSymbol = symbolMapping.getOrDefault(indexSymbol, indexSymbol);

        try {
            // Fetch detailed quote from Finnhub for real change data
            Map<String, Object> quote = getDetailedQuote(actualSymbol);

            benchmark.put("symbol", indexSymbol);
            benchmark.put("value", quote.get("currentPrice"));
            benchmark.put("previousClose", quote.get("previousClose"));
            benchmark.put("change", quote.get("change"));
            benchmark.put("changePercent", quote.get("changePercent"));
            benchmark.put("timestamp", quote.get("timestamp"));

        } catch (Exception e) {
            log.error("Error fetching benchmark with change for {}: {}", indexSymbol, e.getMessage());
            // Fallback to simple calculation
            BigDecimal currentValue = getBenchmarkValue(indexSymbol);
            BigDecimal previousClose = currentValue.multiply(BigDecimal.valueOf(0.995));
            BigDecimal change = currentValue.subtract(previousClose);
            BigDecimal changePercent = change.divide(previousClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            benchmark.put("symbol", indexSymbol);
            benchmark.put("value", currentValue);
            benchmark.put("previousClose", previousClose);
            benchmark.put("change", change);
            benchmark.put("changePercent", changePercent);
            benchmark.put("timestamp", System.currentTimeMillis());
        }

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
                result.put("type", "Stock");

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
     * Search for bonds by symbol or name
     */
    public java.util.List<Map<String, Object>> searchBonds(String query) {
        java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();

        // Mock bond database - in production, this would query a real database or API
        // Format: Symbol, Name, Issuer, Type, CouponRate, CreditRating, Currency
        String[][] bondDatabase = {
                { "US10Y", "US 10-Year Treasury Bond", "US Government", "Government", "4.25", "AAA", "USD" },
                { "US30Y", "US 30-Year Treasury Bond", "US Government", "Government", "4.50", "AAA", "USD" },
                { "US5Y", "US 5-Year Treasury Bond", "US Government", "Government", "4.00", "AAA", "USD" },
                { "US2Y", "US 2-Year Treasury Bond", "US Government", "Government", "4.75", "AAA", "USD" },
                { "AAPL-2030", "Apple Inc. 2030 Bond", "Apple Inc.", "Corporate", "3.50", "AA+", "USD" },
                { "MSFT-2028", "Microsoft Corp 2028 Bond", "Microsoft", "Corporate", "3.25", "AAA", "USD" },
                { "GOOGL-2035", "Alphabet Inc. 2035 Bond", "Alphabet", "Corporate", "3.75", "AA+", "USD" },
                { "JPM-2029", "JPMorgan Chase 2029 Bond", "JPMorgan Chase", "Corporate", "4.00", "A+", "USD" },
                { "IN10Y", "India 10-Year Government Bond", "Government of India", "Government", "7.25", "BBB-", "INR" },
                { "IN5Y", "India 5-Year Government Bond", "Government of India", "Government", "7.00", "BBB-", "INR" },
                { "HDFC-2027", "HDFC Bank 2027 Bond", "HDFC Bank", "Corporate", "7.50", "AAA", "INR" },
                { "ICICI-2026", "ICICI Bank 2026 Bond", "ICICI Bank", "Corporate", "7.25", "AAA", "INR" },
                { "RIL-2030", "Reliance Industries 2030 Bond", "Reliance Industries", "Corporate", "7.75", "AA+", "INR" },
                { "SBI-2028", "SBI 2028 Bond", "State Bank of India", "Corporate", "7.35", "AAA", "INR" },
                { "TATA-2029", "Tata Steel 2029 Bond", "Tata Steel", "Corporate", "8.00", "AA", "INR" },
                { "MUNI-NYC", "New York City Municipal Bond", "NYC", "Municipal", "3.00", "AA", "USD" },
                { "MUNI-CA", "California State Municipal Bond", "California", "Municipal", "3.25", "AA-", "USD" },
                { "TIPS-10Y", "Treasury Inflation-Protected Securities", "US Government", "Government", "1.50", "AAA", "USD" },
                { "GER10Y", "German 10-Year Bund", "German Government", "Government", "2.25", "AAA", "EUR" },
                { "UK10Y", "UK 10-Year Gilt", "UK Government", "Government", "4.00", "AA", "GBP" }
        };

        String searchQuery = query.toLowerCase().trim();

        for (String[] bond : bondDatabase) {
            String symbol = bond[0];
            String name = bond[1];
            String issuer = bond[2];
            String bondType = bond[3];
            String couponRate = bond[4];
            String creditRating = bond[5];
            String currency = bond[6];

            // Fuzzy search by symbol, name, issuer, or type
            if (fuzzyMatch(symbol, searchQuery) ||
                    fuzzyMatch(name, searchQuery) ||
                    fuzzyMatch(issuer, searchQuery) ||
                    fuzzyMatch(bondType, searchQuery)) {

                Map<String, Object> result = new HashMap<>();
                result.put("symbol", symbol);
                result.put("name", name);
                result.put("issuer", issuer);
                result.put("bondType", bondType);
                result.put("couponRate", new BigDecimal(couponRate));
                result.put("creditRating", creditRating);
                result.put("currency", currency);
                result.put("type", "Bond");
                result.put("price", new BigDecimal("100.00")); // Par value for bonds

                results.add(result);

                if (results.size() >= 10) {
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Search for mutual funds by symbol or name
     */
    public java.util.List<Map<String, Object>> searchMutualFunds(String query) {
        java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();

        // Mock mutual fund database
        // Format: Symbol, Name, FundHouse, Category, NAV, ExpenseRatio, RiskLevel, Currency
        String[][] mutualFundDatabase = {
                { "VOO", "Vanguard S&P 500 ETF", "Vanguard", "Index Fund", "450.25", "0.03", "Medium", "USD" },
                { "VTI", "Vanguard Total Stock Market ETF", "Vanguard", "Index Fund", "245.50", "0.03", "Medium", "USD" },
                { "VXUS", "Vanguard Total International Stock ETF", "Vanguard", "International", "58.75", "0.07", "Medium", "USD" },
                { "BND", "Vanguard Total Bond Market ETF", "Vanguard", "Bond Fund", "72.50", "0.03", "Low", "USD" },
                { "SPY", "SPDR S&P 500 ETF Trust", "State Street", "Index Fund", "510.25", "0.09", "Medium", "USD" },
                { "QQQ", "Invesco QQQ Trust", "Invesco", "Technology", "450.75", "0.20", "High", "USD" },
                { "IWM", "iShares Russell 2000 ETF", "BlackRock", "Small Cap", "210.50", "0.19", "High", "USD" },
                { "VWO", "Vanguard FTSE Emerging Markets ETF", "Vanguard", "Emerging Markets", "42.25", "0.08", "High", "USD" },
                { "FXAIX", "Fidelity 500 Index Fund", "Fidelity", "Index Fund", "175.50", "0.015", "Medium", "USD" },
                { "VFIAX", "Vanguard 500 Index Admiral", "Vanguard", "Index Fund", "425.75", "0.04", "Medium", "USD" },
                { "HDFC-EQUITY", "HDFC Equity Fund", "HDFC Mutual Fund", "Large Cap", "850.25", "1.75", "Medium", "INR" },
                { "HDFC-MIDCAP", "HDFC Mid-Cap Opportunities Fund", "HDFC Mutual Fund", "Mid Cap", "125.50", "1.85", "High", "INR" },
                { "ICICI-BLUECHIP", "ICICI Prudential Bluechip Fund", "ICICI Prudential", "Large Cap", "75.25", "1.65", "Medium", "INR" },
                { "SBI-SMALLCAP", "SBI Small Cap Fund", "SBI Mutual Fund", "Small Cap", "145.75", "1.95", "High", "INR" },
                { "AXIS-BLUECHIP", "Axis Bluechip Fund", "Axis Mutual Fund", "Large Cap", "48.50", "1.55", "Low", "INR" },
                { "MIRAE-LARGE", "Mirae Asset Large Cap Fund", "Mirae Asset", "Large Cap", "85.25", "1.45", "Medium", "INR" },
                { "PARAG-FLEXI", "Parag Parikh Flexi Cap Fund", "PPFAS", "Flexi Cap", "62.75", "1.35", "Medium", "INR" },
                { "UTI-NIFTY", "UTI Nifty 50 Index Fund", "UTI Mutual Fund", "Index Fund", "155.50", "0.20", "Medium", "INR" },
                { "NIPPON-INDIA", "Nippon India Growth Fund", "Nippon India", "Multi Cap", "2500.25", "1.75", "High", "INR" },
                { "KOTAK-STD", "Kotak Standard Multicap Fund", "Kotak Mahindra", "Multi Cap", "48.75", "1.55", "Medium", "INR" }
        };

        String searchQuery = query.toLowerCase().trim();

        for (String[] fund : mutualFundDatabase) {
            String symbol = fund[0];
            String name = fund[1];
            String fundHouse = fund[2];
            String category = fund[3];
            String nav = fund[4];
            String expenseRatio = fund[5];
            String riskLevel = fund[6];
            String currency = fund[7];

            // Fuzzy search
            if (fuzzyMatch(symbol, searchQuery) ||
                    fuzzyMatch(name, searchQuery) ||
                    fuzzyMatch(fundHouse, searchQuery) ||
                    fuzzyMatch(category, searchQuery)) {

                Map<String, Object> result = new HashMap<>();
                result.put("symbol", symbol);
                result.put("name", name);
                result.put("fundHouse", fundHouse);
                result.put("category", category);
                result.put("nav", new BigDecimal(nav));
                result.put("expenseRatio", new BigDecimal(expenseRatio));
                result.put("riskLevel", riskLevel);
                result.put("currency", currency);
                result.put("type", "Mutual Fund");
                result.put("price", new BigDecimal(nav));

                results.add(result);

                if (results.size() >= 10) {
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Search for SIPs by symbol or name
     */
    public java.util.List<Map<String, Object>> searchSIPs(String query) {
        java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();

        // Mock SIP database - these are typically mutual funds offered with SIP option
        // Format: Symbol, Name, FundHouse, Category, MinSIP, NAV, RiskLevel, Currency
        String[][] sipDatabase = {
                { "HDFC-SIP-EQUITY", "HDFC Equity Fund - SIP", "HDFC Mutual Fund", "Large Cap", "500", "850.25", "Medium", "INR" },
                { "HDFC-SIP-MIDCAP", "HDFC Mid-Cap Opportunities - SIP", "HDFC Mutual Fund", "Mid Cap", "500", "125.50", "High", "INR" },
                { "ICICI-SIP-BLUE", "ICICI Prudential Bluechip - SIP", "ICICI Prudential", "Large Cap", "500", "75.25", "Medium", "INR" },
                { "SBI-SIP-SMALL", "SBI Small Cap Fund - SIP", "SBI Mutual Fund", "Small Cap", "500", "145.75", "High", "INR" },
                { "AXIS-SIP-BLUE", "Axis Bluechip Fund - SIP", "Axis Mutual Fund", "Large Cap", "500", "48.50", "Low", "INR" },
                { "MIRAE-SIP-LARGE", "Mirae Asset Large Cap - SIP", "Mirae Asset", "Large Cap", "1000", "85.25", "Medium", "INR" },
                { "PARAG-SIP-FLEXI", "Parag Parikh Flexi Cap - SIP", "PPFAS", "Flexi Cap", "1000", "62.75", "Medium", "INR" },
                { "UTI-SIP-NIFTY", "UTI Nifty 50 Index - SIP", "UTI Mutual Fund", "Index Fund", "500", "155.50", "Medium", "INR" },
                { "NIPPON-SIP-GROWTH", "Nippon India Growth - SIP", "Nippon India", "Multi Cap", "100", "2500.25", "High", "INR" },
                { "KOTAK-SIP-STD", "Kotak Standard Multicap - SIP", "Kotak Mahindra", "Multi Cap", "500", "48.75", "Medium", "INR" },
                { "TATA-SIP-DIGITAL", "Tata Digital India Fund - SIP", "Tata Mutual Fund", "Technology", "500", "38.50", "High", "INR" },
                { "ADITYA-SIP-TAX", "Aditya Birla Sun Life Tax Relief - SIP", "Aditya Birla", "ELSS", "500", "45.25", "Medium", "INR" },
                { "DSP-SIP-TAX", "DSP Tax Saver Fund - SIP", "DSP Mutual Fund", "ELSS", "500", "85.75", "Medium", "INR" },
                { "FRANKLIN-SIP-PRIMA", "Franklin India Prima Fund - SIP", "Franklin Templeton", "Mid Cap", "500", "1350.50", "High", "INR" },
                { "SUNDARAM-SIP-MID", "Sundaram Mid Cap Fund - SIP", "Sundaram Mutual Fund", "Mid Cap", "500", "825.25", "High", "INR" },
                { "CANARA-SIP-EQUITY", "Canara Robeco Equity Hybrid - SIP", "Canara Robeco", "Hybrid", "1000", "245.50", "Medium", "INR" },
                { "INVESCO-SIP-GROWTH", "Invesco India Growth Opp - SIP", "Invesco", "Multi Cap", "500", "58.75", "Medium", "INR" },
                { "L&T-SIP-EMERGING", "L&T Emerging Businesses - SIP", "L&T Mutual Fund", "Small Cap", "500", "48.25", "High", "INR" },
                { "MOTILAL-SIP-S&P500", "Motilal Oswal S&P 500 Index - SIP", "Motilal Oswal", "International", "500", "25.50", "Medium", "INR" },
                { "EDELWEISS-SIP-BALANCE", "Edelweiss Balanced Advantage - SIP", "Edelweiss", "Hybrid", "500", "38.25", "Low", "INR" }
        };

        String searchQuery = query.toLowerCase().trim();

        for (String[] sip : sipDatabase) {
            String symbol = sip[0];
            String name = sip[1];
            String fundHouse = sip[2];
            String category = sip[3];
            String minSIP = sip[4];
            String nav = sip[5];
            String riskLevel = sip[6];
            String currency = sip[7];

            // Fuzzy search
            if (fuzzyMatch(symbol, searchQuery) ||
                    fuzzyMatch(name, searchQuery) ||
                    fuzzyMatch(fundHouse, searchQuery) ||
                    fuzzyMatch(category, searchQuery)) {

                Map<String, Object> result = new HashMap<>();
                result.put("symbol", symbol);
                result.put("name", name);
                result.put("fundHouse", fundHouse);
                result.put("category", category);
                result.put("minSIP", new BigDecimal(minSIP));
                result.put("nav", new BigDecimal(nav));
                result.put("riskLevel", riskLevel);
                result.put("currency", currency);
                result.put("type", "SIP");
                result.put("price", new BigDecimal(nav));

                results.add(result);

                if (results.size() >= 10) {
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Search all asset types
     */
    public java.util.List<Map<String, Object>> searchAllAssets(String query, String assetType) {
        if (assetType == null || assetType.isEmpty() || assetType.equalsIgnoreCase("ALL")) {
            // Search all types and combine results
            java.util.List<Map<String, Object>> allResults = new java.util.ArrayList<>();
            allResults.addAll(searchStocks(query));
            allResults.addAll(searchBonds(query));
            allResults.addAll(searchMutualFunds(query));
            allResults.addAll(searchSIPs(query));

            // Sort by relevance and limit
            return allResults.stream()
                    .sorted((a, b) -> {
                        int scoreA = calculateRelevanceScore(a, query);
                        int scoreB = calculateRelevanceScore(b, query);
                        return scoreB - scoreA;
                    })
                    .limit(15)
                    .collect(java.util.stream.Collectors.toList());
        }

        switch (assetType.toUpperCase()) {
            case "STOCK":
                return searchStocks(query);
            case "BOND":
                return searchBonds(query);
            case "MUTUAL_FUND":
                return searchMutualFunds(query);
            case "SIP":
                return searchSIPs(query);
            default:
                return searchStocks(query);
        }
    }

    /**
     * Calculate relevance score for sorting search results
     */
    private int calculateRelevanceScore(Map<String, Object> result, String query) {
        String symbol = ((String) result.get("symbol")).toLowerCase();
        String name = ((String) result.get("name")).toLowerCase();
        String queryLower = query.toLowerCase();

        // Exact match on symbol
        if (symbol.equals(queryLower)) return 100;
        // Symbol starts with query
        if (symbol.startsWith(queryLower)) return 90;
        // Name starts with query
        if (name.startsWith(queryLower)) return 80;
        // Symbol contains query
        if (symbol.contains(queryLower)) return 70;
        // Name contains query
        if (name.contains(queryLower)) return 60;
        // Fuzzy match
        return 50;
    }

    /**
     * Fuzzy match helper using Levenshtein distance
     */
    private boolean fuzzyMatch(String text, String query) {
        if (text == null || query == null) return false;

        String textLower = text.toLowerCase();
        String queryLower = query.toLowerCase();

        // Exact match or contains
        if (textLower.contains(queryLower)) return true;

        // Split query into words for partial matching
        String[] queryWords = queryLower.split("\\s+");
        for (String word : queryWords) {
            if (word.length() >= 2 && textLower.contains(word)) {
                return true;
            }
        }

        // Levenshtein distance for typo tolerance
        if (query.length() >= 3) {
            int distance = levenshteinDistance(textLower, queryLower);
            int maxLength = Math.max(textLower.length(), queryLower.length());
            double similarity = 1.0 - ((double) distance / maxLength);
            return similarity > 0.6; // 60% similarity threshold
        }

        return false;
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + cost
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
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
