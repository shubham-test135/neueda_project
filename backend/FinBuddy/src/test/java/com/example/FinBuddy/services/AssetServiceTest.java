package com.example.FinBuddy.services;

import com.example.FinBuddy.entities.Asset;
import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.entities.Stock;
import com.example.FinBuddy.repositories.AssetRepository;
import com.example.FinBuddy.repositories.PortfolioRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssetService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetService Tests")
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private AssetService assetService;

    private Portfolio testPortfolio;
    private Stock testStock;

    @BeforeEach
    void setUp() {
        // Setup test portfolio
        testPortfolio = new Portfolio();
        testPortfolio.setId(1L);
        testPortfolio.setName("Test Portfolio");
        testPortfolio.setBaseCurrency("USD");

        // Setup test stock
        testStock = new Stock();
        testStock.setId(1L);
        testStock.setName("Apple Inc.");
        testStock.setSymbol("AAPL");
        testStock.setQuantity(10);
        testStock.setPurchasePrice(new BigDecimal("150.00"));
        testStock.setCurrentPrice(new BigDecimal("180.00"));
        testStock.setInvestedAmount(new BigDecimal("1500.00"));
        testStock.setCurrentValue(new BigDecimal("1800.00"));
        testStock.setCurrency("USD");
        testStock.setPurchaseDate(LocalDate.now().minusMonths(6));
        testStock.setPortfolio(testPortfolio);
        testStock.setIsWishlist(false);
    }

    @Test
    @DisplayName("Should create asset successfully")
    void shouldCreateAsset() {
        // Arrange
        Stock newStock = new Stock();
        newStock.setName("Microsoft Corp.");
        newStock.setSymbol("MSFT");
        newStock.setQuantity(5);
        newStock.setPurchasePrice(new BigDecimal("300.00"));
        newStock.setCurrentPrice(new BigDecimal("350.00"));

        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
        when(assetRepository.save(any(Asset.class))).thenReturn(newStock);
        doNothing().when(portfolioService).recalculatePortfolioMetrics(1L);

        // Act
        Asset created = assetService.createAsset(newStock, 1L);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getSymbol()).isEqualTo("MSFT");
        verify(portfolioRepository, times(1)).findById(1L);
        verify(assetRepository, times(1)).save(any(Asset.class));
        verify(portfolioService, times(1)).recalculatePortfolioMetrics(1L);
    }

    @Test
    @DisplayName("Should throw exception when creating asset for non-existent portfolio")
    void shouldThrowExceptionWhenCreatingAssetForNonExistentPortfolio() {
        // Arrange
        when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> assetService.createAsset(testStock, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Portfolio not found");
        verify(portfolioRepository, times(1)).findById(999L);
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    @DisplayName("Should get all assets")
    void shouldGetAllAssets() {
        // Arrange
        List<Asset> assets = Arrays.asList(testStock);
        when(assetRepository.findAll()).thenReturn(assets);

        // Act
        List<Asset> result = assetService.getAllAssets();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
        verify(assetRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get asset by ID")
    void shouldGetAssetById() {
        // Arrange
        when(assetRepository.findById(1L)).thenReturn(Optional.of(testStock));

        // Act
        Optional<Asset> result = assetService.getAssetById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        assertThat(result.get().getName()).isEqualTo("Apple Inc.");
        verify(assetRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when asset not found")
    void shouldReturnEmptyWhenAssetNotFound() {
        // Arrange
        when(assetRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Asset> result = assetService.getAssetById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(assetRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get assets by portfolio")
    void shouldGetAssetsByPortfolio() {
        // Arrange
        List<Asset> assets = Arrays.asList(testStock);
        when(assetRepository.findByPortfolioId(1L)).thenReturn(assets);

        // Act
        List<Asset> result = assetService.getAssetsByPortfolio(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPortfolio().getId()).isEqualTo(1L);
        verify(assetRepository, times(1)).findByPortfolioId(1L);
    }

    @Test
    @DisplayName("Should get invested assets (non-wishlist)")
    void shouldGetInvestedAssets() {
        // Arrange
        List<Asset> assets = Arrays.asList(testStock);
        when(assetRepository.findByPortfolioIdAndIsWishlistFalse(1L)).thenReturn(assets);

        // Act
        List<Asset> result = assetService.getInvestedAssets(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsWishlist()).isFalse();
        verify(assetRepository, times(1)).findByPortfolioIdAndIsWishlistFalse(1L);
    }

    @Test
    @DisplayName("Should get wishlist assets")
    void shouldGetWishlistAssets() {
        // Arrange
        testStock.setIsWishlist(true);
        List<Asset> assets = Arrays.asList(testStock);
        when(assetRepository.findByPortfolioIdAndIsWishlistTrue(1L)).thenReturn(assets);

        // Act
        List<Asset> result = assetService.getWishlistAssets(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsWishlist()).isTrue();
        verify(assetRepository, times(1)).findByPortfolioIdAndIsWishlistTrue(1L);
    }

    @Test
    @DisplayName("Should search assets by name or symbol")
    void shouldSearchAssets() {
        // Arrange
        String searchTerm = "AAPL";
        List<Asset> assets = Arrays.asList(testStock);
        when(assetRepository.searchByNameOrSymbol(searchTerm)).thenReturn(assets);

        // Act
        List<Asset> result = assetService.searchAssets(searchTerm);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).contains("AAPL");
        verify(assetRepository, times(1)).searchByNameOrSymbol(searchTerm);
    }

    @Test
    @DisplayName("Should update asset successfully")
    void shouldUpdateAsset() {
        // Arrange
        Stock updates = new Stock();
        updates.setQuantity(15);
        updates.setCurrentPrice(new BigDecimal("200.00"));

        when(assetRepository.findById(1L)).thenReturn(Optional.of(testStock));
        when(assetRepository.save(any(Asset.class))).thenReturn(testStock);
        when(portfolioService.recalculatePortfolioMetrics(anyLong())).thenReturn(testPortfolio);

        // Act
        Asset updated = assetService.updateAsset(1L, updates);

        // Assert
        assertThat(updated).isNotNull();
        verify(assetRepository, times(1)).findById(1L);
        verify(assetRepository, times(1)).save(any(Asset.class));
        verify(portfolioService, times(1)).recalculatePortfolioMetrics(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent asset")
    void shouldThrowExceptionWhenUpdatingNonExistentAsset() {
        // Arrange
        Stock updates = new Stock();
        when(assetRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> assetService.updateAsset(999L, updates))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Asset not found");
        verify(assetRepository, times(1)).findById(999L);
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    @DisplayName("Should delete asset successfully")
    void shouldDeleteAsset() {
        // Arrange
        when(assetRepository.findById(1L)).thenReturn(Optional.of(testStock));
        doNothing().when(assetRepository).deleteById(1L);
        when(portfolioService.recalculatePortfolioMetrics(anyLong())).thenReturn(testPortfolio);

        // Act
        assetService.deleteAsset(1L);

        // Assert
        verify(assetRepository, times(1)).findById(1L);
        verify(assetRepository, times(1)).deleteById(1L);
        verify(portfolioService, times(1)).recalculatePortfolioMetrics(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent asset")
    void shouldThrowExceptionWhenDeletingNonExistentAsset() {
        // Arrange
        when(assetRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> assetService.deleteAsset(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Asset not found");
        verify(assetRepository, times(1)).findById(999L);
        verify(assetRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should update asset price")
    void shouldUpdateAssetPrice() {
        // Arrange
        BigDecimal newPrice = new BigDecimal("190.00");
        when(assetRepository.findById(1L)).thenReturn(Optional.of(testStock));
        when(assetRepository.save(any(Asset.class))).thenReturn(testStock);
        when(portfolioService.recalculatePortfolioMetrics(anyLong())).thenReturn(testPortfolio);

        // Act
        Asset updated = assetService.updateAssetPrice(1L, newPrice);

        // Assert
        assertThat(updated).isNotNull();
        verify(assetRepository, times(1)).findById(1L);
        verify(assetRepository, times(1)).save(any(Asset.class));
        verify(portfolioService, times(1)).recalculatePortfolioMetrics(anyLong());
    }
}
