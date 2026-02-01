package com.example.FinBuddy.controllers;

import com.example.FinBuddy.services.StockPriceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for MarketDataController
 */
@WebMvcTest(MarketDataController.class)
@DisplayName("MarketDataController Integration Tests")
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockPriceService stockPriceService;

    @Test
    @DisplayName("GET /api/market/price/{symbol} - Should return stock price")
    void shouldGetStockPrice() throws Exception {
        // Arrange
        when(stockPriceService.getRealTimePrice("AAPL")).thenReturn(new BigDecimal("180.50"));

        // Act & Assert
        mockMvc.perform(get("/api/market/price/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.symbol", is("AAPL")))
                .andExpect(jsonPath("$.price", is(180.50)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.source", is("live")));

        verify(stockPriceService, times(1)).getRealTimePrice("AAPL");
    }

    @Test
    @DisplayName("GET /api/market/price/{symbol} - Should handle unavailable price")
    void shouldHandleUnavailablePrice() throws Exception {
        // Arrange
        when(stockPriceService.getRealTimePrice("INVALID")).thenReturn(BigDecimal.ZERO);

        // Act & Assert
        mockMvc.perform(get("/api/market/price/INVALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("INVALID")))
                .andExpect(jsonPath("$.price", is(0)))
                .andExpect(jsonPath("$.source", is("unavailable")));

        verify(stockPriceService, times(1)).getRealTimePrice("INVALID");
    }

    @Test
    @DisplayName("POST /api/market/prices/batch - Should return batch prices")
    void shouldGetBatchPrices() throws Exception {
        // Arrange
        Map<String, BigDecimal> prices = new HashMap<>();
        prices.put("AAPL", new BigDecimal("180.50"));
        prices.put("MSFT", new BigDecimal("350.25"));
        prices.put("GOOGL", new BigDecimal("140.75"));

        when(stockPriceService.getBatchPrices(anyList())).thenReturn(prices);

        // Act & Assert
        mockMvc.perform(post("/api/market/prices/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"AAPL\",\"MSFT\",\"GOOGL\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.AAPL", is(180.50)))
                .andExpect(jsonPath("$.MSFT", is(350.25)))
                .andExpect(jsonPath("$.GOOGL", is(140.75)));

        verify(stockPriceService, times(1)).getBatchPrices(anyList());
    }

    @Test
    @DisplayName("GET /api/market/exchange-rate - Should return exchange rate")
    void shouldGetExchangeRate() throws Exception {
        // Arrange
        when(stockPriceService.getExchangeRate("USD", "EUR")).thenReturn(new BigDecimal("0.92"));

        // Act & Assert
        mockMvc.perform(get("/api/market/exchange-rate")
                .param("from", "USD")
                .param("to", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from", is("USD")))
                .andExpect(jsonPath("$.to", is("EUR")))
                .andExpect(jsonPath("$.rate", is(0.92)));

        verify(stockPriceService, times(1)).getExchangeRate("USD", "EUR");
    }

    @Test
    @DisplayName("GET /api/market/benchmark/{symbol} - Should return benchmark value")
    void shouldGetBenchmarkValue() throws Exception {
        // Arrange
        when(stockPriceService.getBenchmarkValue("^GSPC")).thenReturn(new BigDecimal("4783.45"));

        // Act & Assert
        mockMvc.perform(get("/api/market/benchmark/^GSPC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("^GSPC")))
                .andExpect(jsonPath("$.value", is(4783.45)))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(stockPriceService, times(1)).getBenchmarkValue("^GSPC");
    }

    @Test
    @DisplayName("GET /api/market/quote/{symbol} - Should return detailed quote")
    void shouldGetDetailedQuote() throws Exception {
        // Arrange
        Map<String, Object> quote = new HashMap<>();
        quote.put("symbol", "AAPL");
        quote.put("currentPrice", new BigDecimal("180.50"));
        quote.put("previousClose", new BigDecimal("178.00"));
        quote.put("change", new BigDecimal("2.50"));
        quote.put("changePercent", new BigDecimal("1.40"));
        quote.put("timestamp", System.currentTimeMillis());

        when(stockPriceService.getDetailedQuote("AAPL")).thenReturn(quote);

        // Act & Assert
        mockMvc.perform(get("/api/market/quote/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("AAPL")))
                .andExpect(jsonPath("$.currentPrice", is(180.50)))
                .andExpect(jsonPath("$.change", is(2.50)))
                .andExpect(jsonPath("$.changePercent", is(1.40)));

        verify(stockPriceService, times(1)).getDetailedQuote("AAPL");
    }

    @Test
    @DisplayName("POST /api/market/cache/clear - Should clear cache")
    void shouldClearCache() throws Exception {
        // Arrange
        doNothing().when(stockPriceService).clearCache();

        // Act & Assert
        mockMvc.perform(post("/api/market/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("cleared")));

        verify(stockPriceService, times(1)).clearCache();
    }
}
