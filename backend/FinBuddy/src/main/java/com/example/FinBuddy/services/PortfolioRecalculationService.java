package com.example.FinBuddy.services;

import com.example.FinBuddy.entities.Asset;
import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioRecalculationService {
    private final PortfolioRepository portfolioRepository;
    private final AssetService assetService;
    private final StockPriceService stockPriceService;

    public Portfolio recalculate(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findByIdWithAssets(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        List<String> symbols = portfolio.getAssets().stream()
                .map(Asset::getSymbol)
                .toList();

        Map<String, BigDecimal> prices =
                stockPriceService.getBatchPrices(symbols);

        List<Asset> assetsCopy = List.copyOf(portfolio.getAssets());

        for (Asset asset : assetsCopy) {
            BigDecimal price = prices.get(asset.getSymbol());
            if (price != null) {
                assetService.updateAssetPrice(asset.getId(), price);
            }
        }

        portfolio.recalculateMetrics();
        return portfolioRepository.save(portfolio);
    }
}
