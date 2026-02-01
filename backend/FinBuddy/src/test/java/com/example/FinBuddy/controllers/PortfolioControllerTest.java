package com.example.FinBuddy.controllers;

import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.services.PortfolioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for PortfolioController
 * Uses @WebMvcTest for focused controller testing with MockMvc
 */
@WebMvcTest(PortfolioController.class)
@DisplayName("PortfolioController Integration Tests")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortfolioService portfolioService;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testPortfolio = new Portfolio();
        testPortfolio.setId(1L);
        testPortfolio.setName("Test Portfolio");
        testPortfolio.setDescription("Test Description");
        testPortfolio.setBaseCurrency("USD");
        testPortfolio.setTotalValue(new BigDecimal("10000.00"));
        testPortfolio.setTotalInvestment(new BigDecimal("8000.00"));
        testPortfolio.setTotalGainLoss(new BigDecimal("2000.00"));
        testPortfolio.setGainLossPercentage(new BigDecimal("25.00"));
        testPortfolio.setCreatedAt(LocalDateTime.now());
        testPortfolio.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/portfolios - Should return all portfolios")
    void shouldGetAllPortfolios() throws Exception {
        // Arrange
        List<Portfolio> portfolios = Arrays.asList(testPortfolio);
        when(portfolioService.getAllPortfolios()).thenReturn(portfolios);

        // Act & Assert
        mockMvc.perform(get("/api/portfolios"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test Portfolio")))
                .andExpect(jsonPath("$[0].baseCurrency", is("USD")));

        verify(portfolioService, times(1)).getAllPortfolios();
    }

    @Test
    @DisplayName("GET /api/portfolios/{id} - Should return portfolio by ID")
    void shouldGetPortfolioById() throws Exception {
        // Arrange
        when(portfolioService.getPortfolioById(1L)).thenReturn(Optional.of(testPortfolio));

        // Act & Assert
        mockMvc.perform(get("/api/portfolios/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Portfolio")))
                .andExpect(jsonPath("$.totalValue", is(10000.00)));

        verify(portfolioService, times(1)).getPortfolioById(1L);
    }

    @Test
    @DisplayName("GET /api/portfolios/{id} - Should return 404 when portfolio not found")
    void shouldReturn404WhenPortfolioNotFound() throws Exception {
        // Arrange
        when(portfolioService.getPortfolioById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/portfolios/999"))
                .andExpect(status().isNotFound());

        verify(portfolioService, times(1)).getPortfolioById(999L);
    }

    @Test
    @DisplayName("POST /api/portfolios - Should create new portfolio")
    void shouldCreatePortfolio() throws Exception {
        // Arrange
        Portfolio newPortfolio = new Portfolio();
        newPortfolio.setName("New Portfolio");
        newPortfolio.setBaseCurrency("USD");

        when(portfolioService.createPortfolio(any(Portfolio.class))).thenReturn(testPortfolio);

        // Act & Assert
        mockMvc.perform(post("/api/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPortfolio)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Portfolio")));

        verify(portfolioService, times(1)).createPortfolio(any(Portfolio.class));
    }

    @Test
    @DisplayName("PUT /api/portfolios/{id} - Should update portfolio")
    void shouldUpdatePortfolio() throws Exception {
        // Arrange
        Portfolio updates = new Portfolio();
        updates.setName("Updated Portfolio");
        updates.setDescription("Updated Description");

        when(portfolioService.updatePortfolio(anyLong(), any(Portfolio.class))).thenReturn(testPortfolio);

        // Act & Assert
        mockMvc.perform(put("/api/portfolios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(portfolioService, times(1)).updatePortfolio(anyLong(), any(Portfolio.class));
    }

    @Test
    @DisplayName("DELETE /api/portfolios/{id} - Should delete portfolio")
    void shouldDeletePortfolio() throws Exception {
        // Arrange
        doNothing().when(portfolioService).deletePortfolio(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/portfolios/1"))
                .andExpect(status().isNoContent());

        verify(portfolioService, times(1)).deletePortfolio(1L);
    }

    @Test
    @DisplayName("POST /api/portfolios/{id}/recalculate - Should recalculate metrics")
    void shouldRecalculateMetrics() throws Exception {
        // Arrange
        when(portfolioService.recalculatePortfolioMetrics(1L)).thenReturn(testPortfolio);

        // Act & Assert
        mockMvc.perform(post("/api/portfolios/1/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue", is(10000.00)))
                .andExpect(jsonPath("$.gainLossPercentage", is(25.00)));

        verify(portfolioService, times(1)).recalculatePortfolioMetrics(1L);
    }
}
