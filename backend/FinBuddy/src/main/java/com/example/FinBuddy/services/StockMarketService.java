package com.example.FinBuddy.services;

import com.example.FinBuddy.dto.StockDetailDTO;
import com.example.FinBuddy.dto.StockSearchDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for interacting with stock market data APIs (Finnhub)
 */
@Service
@Slf4j
public class StockMarketService {

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    @Value("${finnhub.api.enabled:true}")
    private boolean apiEnabled;

    private final RestTemplate restTemplate;
    private static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";

    public StockMarketService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Search for stocks, bonds, ETFs by query
     */
    public List<StockSearchDTO> searchSecurities(String query) {
        if (!apiEnabled) {
            return getMockSearchResults(query);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(FINNHUB_BASE_URL + "/search")
                    .queryParam("q", query)
                    .queryParam("token", finnhubApiKey)
                    .toUriString();

            log.info("Searching securities with query: {}", query);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("result")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("result");

                return results.stream()
                        .limit(50) // Limit to top 50 results
                        .map(this::mapToSearchDTO)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("Error searching securities: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Search US stocks specifically
     */
    public List<StockSearchDTO> searchUSStocks(String query) {
        List<StockSearchDTO> allResults = searchSecurities(query);

        // Filter for US exchanges
        return allResults.stream()
                .filter(stock -> isUSExchange(stock.getExchange()))
                .collect(Collectors.toList());
    }

    /**
     * Search bonds
     */
    public List<StockSearchDTO> searchBonds(String query) {
        List<StockSearchDTO> results = searchSecurities(query);

        // Filter for bonds
        return results.stream()
                .filter(item -> item.getType() != null &&
                        (item.getType().toLowerCase().contains("bond") ||
                                item.getType().toLowerCase().contains("treasury") ||
                                item.getType().toLowerCase().contains("debt")))
                .collect(Collectors.toList());
    }

    /**
     * Search ETFs
     */
    public List<StockSearchDTO> searchETFs(String query) {
        List<StockSearchDTO> results = searchSecurities(query);

        // Filter for ETFs
        return results.stream()
                .filter(item -> item.getType() != null &&
                        item.getType().toLowerCase().contains("etf"))
                .collect(Collectors.toList());
    }

    /**
     * Search Mutual Funds
     */
    public List<StockSearchDTO> searchMutualFunds(String query) {
        List<StockSearchDTO> results = searchSecurities(query);

        // Filter for mutual funds
        return results.stream()
                .filter(item -> item.getType() != null &&
                        (item.getType().toLowerCase().contains("mutual") ||
                                item.getType().toLowerCase().contains("fund")))
                .collect(Collectors.toList());
    }

    /**
     * Get detailed stock information
     */
    public StockDetailDTO getStockDetails(String symbol) {
        if (!apiEnabled) {
            return getMockStockDetails(symbol);
        }

        try {
            // Get quote data
            Map<String, Object> quote = getStockQuote(symbol);

            // Get company profile
            Map<String, Object> profile = getCompanyProfile(symbol);

            return mapToStockDetailDTO(symbol, quote, profile);

        } catch (Exception e) {
            log.error("Error fetching stock details for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Get real-time stock quote
     */
    public Map<String, Object> getStockQuote(String symbol) {
        if (!apiEnabled) {
            return getMockQuote(symbol);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(FINNHUB_BASE_URL + "/quote")
                    .queryParam("symbol", symbol)
                    .queryParam("token", finnhubApiKey)
                    .toUriString();

            log.info("Fetching quote for symbol: {}", symbol);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching quote for {}: {}", symbol, e.getMessage());
            return getMockQuote(symbol);
        }
    }

    /**
     * Get company profile
     */
    public Map<String, Object> getCompanyProfile(String symbol) {
        if (!apiEnabled) {
            return getMockProfile(symbol);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(FINNHUB_BASE_URL + "/stock/profile2")
                    .queryParam("symbol", symbol)
                    .queryParam("token", finnhubApiKey)
                    .toUriString();

            log.info("Fetching profile for symbol: {}", symbol);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching profile for {}: {}", symbol, e.getMessage());
            return getMockProfile(symbol);
        }
    }

    /**
     * Get multiple quotes at once (batch operation)
     */
    public Map<String, Map<String, Object>> getBatchQuotes(List<String> symbols) {
        Map<String, Map<String, Object>> quotes = new HashMap<>();

        for (String symbol : symbols) {
            try {
                Map<String, Object> quote = getStockQuote(symbol);
                if (quote != null && !quote.isEmpty()) {
                    quotes.put(symbol, quote);
                }
            } catch (Exception e) {
                log.error("Error fetching quote for symbol {}: {}", symbol, e.getMessage());
            }
        }

        return quotes;
    }

    // Helper methods

    private StockSearchDTO mapToSearchDTO(Map<String, Object> result) {
        return StockSearchDTO.builder()
                .symbol((String) result.get("symbol"))
                .description((String) result.get("description"))
                .displaySymbol((String) result.get("displaySymbol"))
                .type((String) result.get("type"))
                .exchange((String) result.getOrDefault("exchange", ""))
                .mic((String) result.getOrDefault("mic", ""))
                .currency((String) result.getOrDefault("currency", "USD"))
                .build();
    }

    private StockDetailDTO mapToStockDetailDTO(String symbol, Map<String, Object> quote, Map<String, Object> profile) {
        StockDetailDTO.StockDetailDTOBuilder builder = StockDetailDTO.builder()
                .symbol(symbol);

        // From profile
        if (profile != null && !profile.isEmpty()) {
            builder.name((String) profile.get("name"))
                    .exchange((String) profile.get("exchange"))
                    .currency((String) profile.get("currency"))
                    .sector((String) profile.get("finnhubIndustry"))
                    .industry((String) profile.get("finnhubIndustry"))
                    .country((String) profile.get("country"))
                    .logo((String) profile.get("logo"))
                    .weburl((String) profile.get("weburl"));

            if (profile.containsKey("marketCapitalization")) {
                builder.marketCap(formatMarketCap(profile.get("marketCapitalization")));
            }
        }

        // From quote
        if (quote != null && !quote.isEmpty()) {
            BigDecimal current = getBigDecimal(quote.get("c"));
            BigDecimal previous = getBigDecimal(quote.get("pc"));

            builder.currentPrice(current)
                    .previousClose(previous)
                    .open(getBigDecimal(quote.get("o")))
                    .high(getBigDecimal(quote.get("h")))
                    .low(getBigDecimal(quote.get("l")));

            if (current != null && previous != null && previous.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = current.subtract(previous);
                builder.change(change);

                BigDecimal changePercent = change
                        .divide(previous, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                builder.changePercent(changePercent);
            }
        }

        return builder.build();
    }

    private boolean isUSExchange(String exchange) {
        if (exchange == null) return false;
        String[] usExchanges = {"NASDAQ", "NYSE", "AMEX", "US", "NYSEARCA", "BATS", "OTC"};
        return Arrays.stream(usExchanges)
                .anyMatch(ex -> exchange.toUpperCase().contains(ex));
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatMarketCap(Object value) {
        if (value == null) return "N/A";
        try {
            double marketCap = Double.parseDouble(value.toString());
            if (marketCap >= 1000) {
                return String.format("$%.2fT", marketCap / 1000);
            } else if (marketCap >= 1) {
                return String.format("$%.2fB", marketCap);
            } else {
                return String.format("$%.2fM", marketCap * 1000);
            }
        } catch (Exception e) {
            return "N/A";
        }
    }

    // Mock data methods for testing

    private List<StockSearchDTO> getMockSearchResults(String query) {
        List<StockSearchDTO> results = new ArrayList<>();

        results.add(StockSearchDTO.builder()
                .symbol("AAPL").description("Apple Inc").displaySymbol("AAPL")
                .type("Common Stock").currency("USD").exchange("NASDAQ").mic("XNAS").build());
        results.add(StockSearchDTO.builder()
                .symbol("GOOGL").description("Alphabet Inc Class A").displaySymbol("GOOGL")
                .type("Common Stock").currency("USD").exchange("NASDAQ").mic("XNAS").build());
        results.add(StockSearchDTO.builder()
                .symbol("MSFT").description("Microsoft Corporation").displaySymbol("MSFT")
                .type("Common Stock").currency("USD").exchange("NASDAQ").mic("XNAS").build());
        results.add(StockSearchDTO.builder()
                .symbol("SPY").description("SPDR S&P 500 ETF Trust").displaySymbol("SPY")
                .type("ETF").currency("USD").exchange("NYSE").mic("XNYS").build());
        results.add(StockSearchDTO.builder()
                .symbol("TLT").description("iShares 20+ Year Treasury Bond ETF").displaySymbol("TLT")
                .type("Bond ETF").currency("USD").exchange("NASDAQ").mic("XNAS").build());

        return results.stream()
                .filter(s -> s.getSymbol().toLowerCase().contains(query.toLowerCase()) ||
                        s.getDescription().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> getMockQuote(String symbol) {
        Map<String, Object> quote = new HashMap<>();
        Random random = new Random();
        double basePrice = 150.00 + random.nextDouble() * 50;

        quote.put("c", basePrice); // current
        quote.put("pc", basePrice * 0.99); // previous close
        quote.put("o", basePrice * 0.995); // open
        quote.put("h", basePrice * 1.02); // high
        quote.put("l", basePrice * 0.98); // low
        quote.put("t", System.currentTimeMillis() / 1000); // timestamp

        return quote;
    }

    private Map<String, Object> getMockProfile(String symbol) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", symbol + " Company");
        profile.put("exchange", "NASDAQ");
        profile.put("currency", "USD");
        profile.put("finnhubIndustry", "Technology");
        profile.put("marketCapitalization", 2500.0);
        profile.put("country", "US");
        profile.put("weburl", "https://example.com");

        return profile;
    }

    private StockDetailDTO getMockStockDetails(String symbol) {
        Random random = new Random();
        double basePrice = 150.00 + random.nextDouble() * 50;

        return StockDetailDTO.builder()
                .symbol(symbol)
                .name(symbol + " Company")
                .exchange("NASDAQ")
                .currency("USD")
                .type("Common Stock")
                .currentPrice(new BigDecimal(basePrice).setScale(2, RoundingMode.HALF_UP))
                .previousClose(new BigDecimal(basePrice * 0.99).setScale(2, RoundingMode.HALF_UP))
                .change(new BigDecimal(basePrice * 0.01).setScale(2, RoundingMode.HALF_UP))
                .changePercent(new BigDecimal("1.01"))
                .marketCap("$2.50T")
                .sector("Technology")
                .build();
    }
}