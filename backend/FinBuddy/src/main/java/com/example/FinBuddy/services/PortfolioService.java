package com.example.FinBuddy.services;

import com.example.FinBuddy.dto.AssetAllocationDTO;
import com.example.FinBuddy.dto.AssetPerformanceDTO;
import com.example.FinBuddy.dto.DashboardSummaryDTO;
import com.example.FinBuddy.dto.PerformanceDataDTO;
import com.example.FinBuddy.entities.Asset;
import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.entities.PortfolioHistory;
import com.example.FinBuddy.repositories.AssetRepository;
import com.example.FinBuddy.repositories.PortfolioHistoryRepository;
import com.example.FinBuddy.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for Portfolio management
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final PortfolioHistoryRepository portfolioHistoryRepository;

    /**
     * Create a new portfolio
     */
    public Portfolio createPortfolio(Portfolio portfolio) {
        portfolio.setCreatedAt(LocalDateTime.now());
        portfolio.setUpdatedAt(LocalDateTime.now());
        return portfolioRepository.save(portfolio);
    }

    /**
     * Get all portfolios
     */
    @Transactional(readOnly = true)
    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get portfolio by ID
     */
    @Transactional(readOnly = true)
    public Optional<Portfolio> getPortfolioById(Long id) {
        return portfolioRepository.findById(id);
    }

    /**
     * Get portfolio with all assets
     */
    @Transactional(readOnly = true)
    public Optional<Portfolio> getPortfolioWithAssets(Long id) {
        return portfolioRepository.findByIdWithAssets(id);
    }

    /**
     * Update portfolio
     */
    public Portfolio updatePortfolio(Long id, Portfolio portfolioDetails) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        portfolio.setName(portfolioDetails.getName());
        portfolio.setDescription(portfolioDetails.getDescription());
        portfolio.setBaseCurrency(portfolioDetails.getBaseCurrency());
        portfolio.setUpdatedAt(LocalDateTime.now());

        return portfolioRepository.save(portfolio);
    }

    /**
     * Delete portfolio
     */
    public void deletePortfolio(Long id) {
        portfolioRepository.deleteById(id);
    }

    /**
     * Recalculate and update portfolio metrics
     */
    public Portfolio recalculatePortfolioMetrics(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findByIdWithAssets(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // Recalculate each asset's metrics first
        portfolio.getAssets().forEach(Asset::calculateMetrics);

        // Then recalculate portfolio metrics
        portfolio.recalculateMetrics();

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        // Save portfolio history snapshot
        savePortfolioSnapshot(savedPortfolio);

        return savedPortfolio;
    }

    /**
     * Save portfolio snapshot for history tracking
     */
    private void savePortfolioSnapshot(Portfolio portfolio) {
        PortfolioHistory history = new PortfolioHistory();
        history.setPortfolio(portfolio);
        history.setRecordDate(LocalDate.now());
        history.setTotalValue(portfolio.getTotalValue());
        history.setTotalInvestment(portfolio.getTotalInvestment());
        history.setGainLoss(portfolio.getTotalGainLoss());
        history.setGainLossPercentage(portfolio.getGainLossPercentage());

        portfolioHistoryRepository.save(history);
    }

    /**
     * Get dashboard summary for a portfolio
     */
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findByIdWithAssets(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        List<Asset> assets = portfolio.getAssets();
        List<Asset> wishlistAssets = assets.stream()
                .filter(Asset::getIsWishlist)
                .collect(Collectors.toList());

        DashboardSummaryDTO dashboard = new DashboardSummaryDTO();
        dashboard.setPortfolioId(portfolio.getId());
        dashboard.setPortfolioName(portfolio.getName());
        dashboard.setTotalValue(portfolio.getTotalValue());
        dashboard.setTotalInvestment(portfolio.getTotalInvestment());
        dashboard.setTotalGainLoss(portfolio.getTotalGainLoss());
        dashboard.setGainLossPercentage(portfolio.getGainLossPercentage());
        dashboard.setBaseCurrency(portfolio.getBaseCurrency());
        dashboard.setAssetCount(assets.size() - wishlistAssets.size());
        dashboard.setWishlistCount(wishlistAssets.size());

        // Calculate asset allocation
        dashboard.setAssetAllocation(calculateAssetAllocation(assets));

        // Get top performers
        dashboard.setTopPerformers(getTopPerformers(assets, 5));

        // Get performance data
        dashboard.setPerformanceData(calculatePerformanceData(portfolio));

        return dashboard;
    }

    /**
     * Calculate asset allocation by type
     */
    private List<AssetAllocationDTO> calculateAssetAllocation(List<Asset> assets) {
        List<Asset> investedAssets = assets.stream()
                .filter(a -> !a.getIsWishlist())
                .collect(Collectors.toList());

        BigDecimal totalValue = investedAssets.stream()
                .map(Asset::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        return investedAssets.stream()
                .collect(Collectors.groupingBy(Asset::getAssetType))
                .entrySet()
                .stream()
                .map(entry -> {
                    BigDecimal typeValue = entry.getValue().stream()
                            .map(Asset::getCurrentValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    AssetAllocationDTO dto = new AssetAllocationDTO();
                    dto.setAssetType(entry.getKey());
                    dto.setTotalValue(typeValue);
                    dto.setPercentage(
                            typeValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)));
                    dto.setCount(entry.getValue().size());

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate performance data for chart
     */
    private PerformanceDataDTO calculatePerformanceData(Portfolio portfolio) {
        // Get last 30 days of history
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<PortfolioHistory> history = portfolioHistoryRepository
                .findByPortfolioIdAndRecordDateBetweenOrderByRecordDateAsc(
                        portfolio.getId(), startDate, endDate);

        PerformanceDataDTO performanceData = new PerformanceDataDTO();

        if (history.isEmpty()) {
            // Generate sample data for the last 7 days if no history exists
            List<String> dates = new ArrayList<>();
            List<BigDecimal> values = new ArrayList<>();

            BigDecimal baseValue = portfolio.getTotalValue();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                dates.add(date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd")));

                // Simulate slight variations (±2% from current value)
                double variance = 0.98 + (Math.random() * 0.04);
                values.add(baseValue.multiply(BigDecimal.valueOf(variance)));
            }

            performanceData.setDates(dates);
            performanceData.setValues(values);
        } else {
            performanceData.setDates(
                    history.stream()
                            .map(h -> h.getRecordDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd")))
                            .collect(Collectors.toList()));
            performanceData.setValues(
                    history.stream()
                            .map(PortfolioHistory::getTotalValue)
                            .collect(Collectors.toList()));
        }

        return performanceData;
    }

    /**
     * Get top performing assets
     */
    private List<AssetPerformanceDTO> getTopPerformers(List<Asset> assets, int limit) {
        return assets.stream()
                .filter(a -> !a.getIsWishlist())
                .sorted((a1, a2) -> a2.getGainLossPercentage().compareTo(a1.getGainLossPercentage()))
                .limit(limit)
                .map(asset -> {
                    AssetPerformanceDTO dto = new AssetPerformanceDTO();
                    dto.setAssetId(asset.getId());
                    dto.setName(asset.getName());
                    dto.setSymbol(asset.getSymbol());
                    dto.setAssetType(asset.getAssetType());
                    dto.setCurrentValue(asset.getCurrentValue());
                    dto.setGainLoss(asset.getGainLoss());
                    dto.setGainLossPercentage(asset.getGainLossPercentage());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get portfolio performance history
     */
    @Transactional(readOnly = true)
    public List<PortfolioHistory> getPortfolioHistory(Long portfolioId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return portfolioHistoryRepository.findByPortfolioIdAndRecordDateBetweenOrderByRecordDateAsc(
                    portfolioId, startDate, endDate);
        }
        return portfolioHistoryRepository.findByPortfolioIdOrderByRecordDateAsc(portfolioId);
    }
}
