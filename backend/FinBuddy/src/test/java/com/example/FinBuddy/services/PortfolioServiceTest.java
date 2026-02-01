package com.example.FinBuddy.services;

import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.entities.Asset;
import com.example.FinBuddy.entities.Stock;
import com.example.FinBuddy.repositories.PortfolioRepository;
import com.example.FinBuddy.repositories.AssetRepository;
import com.example.FinBuddy.repositories.PortfolioHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Tests")
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PortfolioHistoryRepository portfolioHistoryRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private Portfolio testPortfolio;
    private List<Asset> testAssets;

    @BeforeEach
    void setUp() {
        // Setup test portfolio
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

        // Setup test assets
        testAssets = new ArrayList<>();
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setName("Apple Inc.");
        stock.setSymbol("AAPL");
        stock.setQuantity(10);
        stock.setPurchasePrice(new BigDecimal("150.00"));
        stock.setCurrentPrice(new BigDecimal("180.00"));
        stock.setInvestedAmount(new BigDecimal("1500.00"));
        stock.setCurrentValue(new BigDecimal("1800.00"));
        stock.setPurchaseDate(LocalDate.now().minusMonths(6));
        stock.setPortfolio(testPortfolio);
        testAssets.add(stock);

        testPortfolio.setAssets(testAssets);
    }

    @Test
    @DisplayName("Should create portfolio successfully")
    void shouldCreatePortfolio() {
        // Arrange
        Portfolio newPortfolio = new Portfolio();
        newPortfolio.setName("New Portfolio");
        newPortfolio.setBaseCurrency("USD");

        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // Act
        Portfolio created = portfolioService.createPortfolio(newPortfolio);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getName()).isEqualTo("Test Portfolio");
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("Should get all portfolios ordered by created date")
    void shouldGetAllPortfolios() {
        // Arrange
        List<Portfolio> portfolios = List.of(testPortfolio);
        when(portfolioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(portfolios);

        // Act
        List<Portfolio> result = portfolioService.getAllPortfolios();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Portfolio");
        verify(portfolioRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should get portfolio by ID")
    void shouldGetPortfolioById() {
        // Arrange
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));

        // Act
        Optional<Portfolio> result = portfolioService.getPortfolioById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Portfolio");
        verify(portfolioRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when portfolio not found")
    void shouldReturnEmptyWhenPortfolioNotFound() {
        // Arrange
        when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Portfolio> result = portfolioService.getPortfolioById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(portfolioRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should update portfolio successfully")
    void shouldUpdatePortfolio() {
        // Arrange
        Portfolio updates = new Portfolio();
        updates.setName("Updated Portfolio");
        updates.setDescription("Updated Description");
        updates.setBaseCurrency("EUR");

        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // Act
        Portfolio updated = portfolioService.updatePortfolio(1L, updates);

        // Assert
        assertThat(updated).isNotNull();
        verify(portfolioRepository, times(1)).findById(1L);
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent portfolio")
    void shouldThrowExceptionWhenUpdatingNonExistentPortfolio() {
        // Arrange
        Portfolio updates = new Portfolio();
        when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> portfolioService.updatePortfolio(999L, updates))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Portfolio not found");
        verify(portfolioRepository, times(1)).findById(999L);
        verify(portfolioRepository, never()).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("Should delete portfolio successfully")
    void shouldDeletePortfolio() {
        // Arrange
        doNothing().when(portfolioRepository).deleteById(1L);

        // Act
        portfolioService.deletePortfolio(1L);

        // Assert
        verify(portfolioRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should get portfolio with assets")
    void shouldGetPortfolioWithAssets() {
        // Arrange
        when(portfolioRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(testPortfolio));

        // Act
        Optional<Portfolio> result = portfolioService.getPortfolioWithAssets(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAssets()).hasSize(1);
        assertThat(result.get().getAssets().get(0).getSymbol()).isEqualTo("AAPL");
        verify(portfolioRepository, times(1)).findByIdWithAssets(1L);
    }

    @Test
    @DisplayName("Should recalculate portfolio metrics")
    void shouldRecalculatePortfolioMetrics() {
        // Arrange
        when(portfolioRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // Act
        Portfolio result = portfolioService.recalculatePortfolioMetrics(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(portfolioRepository, times(1)).findByIdWithAssets(1L);
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("Should throw exception when recalculating metrics for non-existent portfolio")
    void shouldThrowExceptionWhenRecalculatingNonExistentPortfolio() {
        // Arrange
        when(portfolioRepository.findByIdWithAssets(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> portfolioService.recalculatePortfolioMetrics(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Portfolio not found");
        verify(portfolioRepository, times(1)).findByIdWithAssets(999L);
    }
}
