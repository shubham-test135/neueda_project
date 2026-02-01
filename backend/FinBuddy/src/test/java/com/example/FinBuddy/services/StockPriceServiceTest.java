package com.example.FinBuddy.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StockPriceService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockPriceService Tests")
class StockPriceServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private StockPriceService stockPriceService;

    private String apiKey = "test-api-key";
    private String alphaVantageKey = "test-av-key";
    private boolean apiEnabled = true;

    @BeforeEach
    void setUp() {
        // Setup WebClient builder mock chain
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // Reinitialize service with mocked dependencies
        stockPriceService = new StockPriceService(webClientBuilder, apiKey, alphaVantageKey, apiEnabled);
    }

    @Test
    @DisplayName("Should return mock price when API is disabled")
    void shouldReturnMockPriceWhenApiDisabled() {
        // Arrange
        stockPriceService = new StockPriceService(webClientBuilder, "demo", "demo", false);

        // Act
        BigDecimal price = stockPriceService.getRealTimePrice("AAPL");

        // Assert
        assertThat(price).isNotNull();
        assertThat(price).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return mock price for invalid symbol")
    void shouldReturnMockPriceForInvalidSymbol() {
        // Arrange - API returns null for invalid symbol
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());

        // Act
        BigDecimal price = stockPriceService.getRealTimePrice("INVALID");

        // Assert
        assertThat(price).isNotNull();
        assertThat(price).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should get exchange rate")
    void shouldGetExchangeRate() {
        // Act
        BigDecimal usdToEur = stockPriceService.getExchangeRate("USD", "EUR");
        BigDecimal usdToUsd = stockPriceService.getExchangeRate("USD", "USD");

        // Assert
        assertThat(usdToEur).isNotNull();
        assertThat(usdToEur).isGreaterThan(BigDecimal.ZERO);
        assertThat(usdToUsd).isEqualTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Should get benchmark value")
    void shouldGetBenchmarkValue() {
        // Act
        BigDecimal sp500 = stockPriceService.getBenchmarkValue("^GSPC");
        BigDecimal nifty = stockPriceService.getBenchmarkValue("^NSEI");

        // Assert
        assertThat(sp500).isNotNull();
        assertThat(sp500).isGreaterThan(BigDecimal.ZERO);
        assertThat(nifty).isNotNull();
        assertThat(nifty).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should get detailed quote")
    void shouldGetDetailedQuote() {
        // Act
        Map<String, Object> quote = stockPriceService.getDetailedQuote("AAPL");

        // Assert
        assertThat(quote).isNotNull();
        assertThat(quote).containsKeys("symbol", "currentPrice", "previousClose", "change", "changePercent",
                "timestamp");
        assertThat(quote.get("symbol")).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("Should get batch prices")
    void shouldGetBatchPrices() {
        // Arrange
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");

        // Act
        Map<String, BigDecimal> prices = stockPriceService.getBatchPrices(symbols);

        // Assert
        assertThat(prices).isNotNull();
        assertThat(prices).hasSize(3);
        assertThat(prices).containsKeys("AAPL", "MSFT", "GOOGL");
        prices.values().forEach(price -> {
            assertThat(price).isGreaterThan(BigDecimal.ZERO);
        });
    }

    @Test
    @DisplayName("Should clear cache successfully")
    void shouldClearCache() {
        // Act - Should not throw exception
        stockPriceService.clearCache();

        // Assert - Verify cache is cleared by getting fresh price
        BigDecimal price1 = stockPriceService.getRealTimePrice("AAPL");
        stockPriceService.clearCache();
        BigDecimal price2 = stockPriceService.getRealTimePrice("AAPL");

        assertThat(price1).isNotNull();
        assertThat(price2).isNotNull();
    }

    @Test
    @DisplayName("Should cache prices for same symbol")
    void shouldCachePricesForSameSymbol() {
        // Act
        BigDecimal price1 = stockPriceService.getRealTimePrice("AAPL");
        BigDecimal price2 = stockPriceService.getRealTimePrice("AAPL");

        // Assert - Cached price should be same
        assertThat(price1).isEqualTo(price2);
    }

    @Test
    @DisplayName("Should return different mock prices for different symbols")
    void shouldReturnDifferentMockPricesForDifferentSymbols() {
        // Arrange
        stockPriceService = new StockPriceService(webClientBuilder, "demo", "demo", false);

        // Act
        BigDecimal applePrice = stockPriceService.getRealTimePrice("AAPL");
        BigDecimal microsoftPrice = stockPriceService.getRealTimePrice("MSFT");

        // Assert - Different symbols should have different prices
        assertThat(applePrice).isNotEqualTo(microsoftPrice);
    }

    @Test
    @DisplayName("Should handle reverse currency conversion")
    void shouldHandleReverseCurrencyConversion() {
        // Act
        BigDecimal usdToInr = stockPriceService.getExchangeRate("USD", "INR");
        BigDecimal inrToUsd = stockPriceService.getExchangeRate("INR", "USD");

        // Assert - Should be inverse of each other
        assertThat(usdToInr).isGreaterThan(BigDecimal.ONE);
        assertThat(inrToUsd).isLessThan(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Should return zero for unknown benchmark")
    void shouldReturnZeroForUnknownBenchmark() {
        // Act
        BigDecimal unknownBenchmark = stockPriceService.getBenchmarkValue("UNKNOWN");

        // Assert
        assertThat(unknownBenchmark).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should calculate price change in detailed quote")
    void shouldCalculatePriceChangeInDetailedQuote() {
        // Act
        Map<String, Object> quote = stockPriceService.getDetailedQuote("AAPL");

        // Assert
        BigDecimal currentPrice = (BigDecimal) quote.get("currentPrice");
        BigDecimal previousClose = (BigDecimal) quote.get("previousClose");
        BigDecimal change = (BigDecimal) quote.get("change");
        BigDecimal changePercent = (BigDecimal) quote.get("changePercent");

        assertThat(change).isEqualTo(currentPrice.subtract(previousClose));
        assertThat(changePercent).isNotNull();
    }
}
