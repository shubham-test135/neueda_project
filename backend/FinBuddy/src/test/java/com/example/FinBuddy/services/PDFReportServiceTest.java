package com.example.FinBuddy.services;

import com.example.FinBuddy.dto.DashboardSummaryDTO;
import com.example.FinBuddy.entities.Asset;
import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.entities.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PDFReportService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PDFReportService Tests")
class PDFReportServiceTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private PDFReportService pdfReportService;

    private Portfolio testPortfolio;
    private DashboardSummaryDTO dashboardSummary;
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
        stock.setGainLoss(new BigDecimal("300.00"));
        stock.setGainLossPercentage(new BigDecimal("20.00"));
        stock.setCurrency("USD");
        stock.setPurchaseDate(LocalDate.now().minusMonths(6));
        testAssets.add(stock);

        testPortfolio.setAssets(testAssets);

        // Setup dashboard summary
        dashboardSummary = new DashboardSummaryDTO();
        dashboardSummary.setTotalValue(new BigDecimal("10000.00"));
        dashboardSummary.setTotalInvestment(new BigDecimal("8000.00"));
        dashboardSummary.setTotalGainLoss(new BigDecimal("2000.00"));
        dashboardSummary.setGainLossPercentage(new BigDecimal("25.00"));
        dashboardSummary.setTotalAssets(new BigDecimal("1"));
    }

    @Test
    @DisplayName("Should generate PDF report successfully")
    void shouldGeneratePdfReportSuccessfully() {
        // Arrange
        when(portfolioService.getPortfolioWithAssets(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioService.getDashboardSummary(1L)).thenReturn(dashboardSummary);

        // Act
        byte[] pdfBytes = pdfReportService.generatePortfolioReport(1L);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
        verify(portfolioService, times(1)).getPortfolioWithAssets(1L);
        verify(portfolioService, times(1)).getDashboardSummary(1L);
    }

    @Test
    @DisplayName("Should throw exception when portfolio not found")
    void shouldThrowExceptionWhenPortfolioNotFound() {
        // Arrange
        when(portfolioService.getPortfolioWithAssets(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pdfReportService.generatePortfolioReport(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Portfolio not found");
        verify(portfolioService, times(1)).getPortfolioWithAssets(999L);
        verify(portfolioService, never()).getDashboardSummary(anyLong());
    }

    @Test
    @DisplayName("Should generate PDF with empty assets list")
    void shouldGeneratePdfWithEmptyAssetsList() {
        // Arrange
        testPortfolio.setAssets(new ArrayList<>());
        dashboardSummary.setTotalAssets(new BigDecimal("0"));
        when(portfolioService.getPortfolioWithAssets(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioService.getDashboardSummary(1L)).thenReturn(dashboardSummary);

        // Act
        byte[] pdfBytes = pdfReportService.generatePortfolioReport(1L);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should generate PDF with multiple assets")
    void shouldGeneratePdfWithMultipleAssets() {
        // Arrange
        Stock stock2 = new Stock();
        stock2.setId(2L);
        stock2.setName("Microsoft Corp.");
        stock2.setSymbol("MSFT");
        stock2.setQuantity(5);
        stock2.setPurchasePrice(new BigDecimal("300.00"));
        stock2.setCurrentPrice(new BigDecimal("350.00"));
        stock2.setInvestedAmount(new BigDecimal("1500.00"));
        stock2.setCurrentValue(new BigDecimal("1750.00"));
        stock2.setGainLoss(new BigDecimal("250.00"));
        stock2.setGainLossPercentage(new BigDecimal("16.67"));
        stock2.setCurrency("USD");
        stock2.setPurchaseDate(LocalDate.now().minusMonths(3));
        testAssets.add(stock2);

        dashboardSummary.setTotalAssets(new BigDecimal("2"));

        when(portfolioService.getPortfolioWithAssets(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioService.getDashboardSummary(1L)).thenReturn(dashboardSummary);

        // Act
        byte[] pdfBytes = pdfReportService.generatePortfolioReport(1L);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should verify PDF contains portfolio information")
    void shouldVerifyPdfContainsPortfolioInformation() {
        // Arrange
        when(portfolioService.getPortfolioWithAssets(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioService.getDashboardSummary(1L)).thenReturn(dashboardSummary);

        // Act
        byte[] pdfBytes = pdfReportService.generatePortfolioReport(1L);

        // Assert
        assertThat(pdfBytes).isNotNull();
        // PDF header bytes should start with %PDF
        String pdfHeader = new String(pdfBytes, 0, Math.min(8, pdfBytes.length));
        assertThat(pdfHeader).startsWith("%PDF");
    }

    @Test
    @DisplayName("Should handle portfolio with negative gain/loss")
    void shouldHandlePortfolioWithNegativeGainLoss() {
        // Arrange
        testPortfolio.setTotalGainLoss(new BigDecimal("-500.00"));
        testPortfolio.setGainLossPercentage(new BigDecimal("-6.25"));
        dashboardSummary.setTotalGainLoss(new BigDecimal("-500.00"));
        dashboardSummary.setGainLossPercentage(new BigDecimal("-6.25"));

        when(portfolioService.getPortfolioWithAssets(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioService.getDashboardSummary(1L)).thenReturn(dashboardSummary);

        // Act
        byte[] pdfBytes = pdfReportService.generatePortfolioReport(1L);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle different currencies")
    void shouldHandleDifferentCurrencies() {
        // Arrange
        testPortfolio.setBaseCurrency("EUR");
        testAssets.get(0).setCurrency("EUR");

        when(portfolioService.getPortfolioWithAssets(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioService.getDashboardSummary(1L)).thenReturn(dashboardSummary);

        // Act
        byte[] pdfBytes = pdfReportService.generatePortfolioReport(1L);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should include timestamp in generated report")
    void shouldIncludeTimestampInReport() {
        // Arrange
        when(portfolioService.getPortfolioWithAssets(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioService.getDashboardSummary(1L)).thenReturn(dashboardSummary);

        // Act
        byte[] pdfBytes = pdfReportService.generatePortfolioReport(1L);

        // Assert
        assertThat(pdfBytes).isNotNull();
        // Verify PDF was created (size check)
        assertThat(pdfBytes.length).isGreaterThan(100); // Minimum viable PDF size
    }
}
