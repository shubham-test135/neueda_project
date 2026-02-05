package com.example.FinBuddy.services;

import com.example.FinBuddy.dto.AddWishlistItemRequest;
import com.example.FinBuddy.dto.WishlistItemDTO;
import com.example.FinBuddy.dto.WishlistSummaryDTO;
import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.entities.WishlistItem;
import com.example.FinBuddy.exceptions.DuplicateResourceException;
import com.example.FinBuddy.exceptions.ExternalServiceException;
import com.example.FinBuddy.exceptions.ResourceNotFoundException;
import com.example.FinBuddy.exceptions.UnauthorizedAccessException;
import com.example.FinBuddy.repositories.PortfolioRepository;
import com.example.FinBuddy.repositories.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockPriceService stockPriceService;

    /**
     * Add item to wishlist (Portfolio-based)
     */
    @Transactional
    public WishlistItemDTO addToWishlist(Long portfolioId, AddWishlistItemRequest request) {

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", portfolioId));

        String symbol = request.getSymbol().toUpperCase();

        if (wishlistRepository.existsByPortfolioAndSymbol(portfolio, symbol)) {
            throw new DuplicateResourceException("Wishlist item", "symbol", symbol);
        }

        Map<String, Object> stockData;
        try {
            stockData = stockPriceService.fetchStockData(symbol);
        } catch (Exception e) {
            log.error("Failed to fetch stock data for symbol: {}", symbol, e);
            throw new ExternalServiceException("Stock Price Service",
                    "Failed to fetch stock data for symbol: " + symbol, e);
        }

        BigDecimal currentPrice = (BigDecimal) stockData.get("currentPrice");

        WishlistItem item = new WishlistItem();
        item.setPortfolio(portfolio);
        item.setSymbol(symbol);
        item.setName((String) stockData.getOrDefault("name", symbol));
        item.setCategory(request.getCategory().toUpperCase());
        item.setTargetPrice(request.getTargetPrice());
        item.setPriceWhenAdded(currentPrice);
        item.setCurrentPrice(currentPrice);
        item.setChangeAmount((BigDecimal) stockData.get("changeAmount"));
        item.setChangePercentage((BigDecimal) stockData.get("changePercentage"));
        item.setAlertEnabled(request.getTargetPrice() != null);

        WishlistItem saved = wishlistRepository.save(item);

        log.info("Added {} to wishlist for portfolio {}", symbol, portfolioId);
        return convertToDTO(saved);
    }

    /**
     * Get wishlist for portfolio
     */
    @Transactional(readOnly = true)
    public List<WishlistItemDTO> getWishlist(Long portfolioId) {

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", portfolioId));

        return wishlistRepository.findByPortfolioOrderByAddedAtDesc(portfolio)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update wishlist item
     */
    @Transactional
    public WishlistItemDTO updateWishlistItem(
            Long portfolioId,
            Long itemId,
            BigDecimal targetPrice,
            String notes) {

        WishlistItem item = getValidatedItem(portfolioId, itemId);

        if (targetPrice != null) {
            item.setTargetPrice(targetPrice);
            item.setAlertEnabled(true);
            item.setAlertTriggered(false);
        }

        WishlistItem updated = wishlistRepository.save(item);
        return convertToDTO(updated);
    }

    /**
     * Remove from wishlist
     */
    @Transactional
    public void removeFromWishlist(Long portfolioId, Long itemId) {

        WishlistItem item = getValidatedItem(portfolioId, itemId);
        wishlistRepository.delete(item);

        log.info("Removed {} from wishlist (portfolio {})",
                item.getSymbol(), portfolioId);
    }

    /**
     * Refresh prices for wishlist
     */
    @Transactional
    public List<WishlistItemDTO> refreshPrices(Long portfolioId) {

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", portfolioId));

        List<WishlistItem> items = wishlistRepository.findByPortfolioOrderByAddedAtDesc(portfolio);

        for (WishlistItem item : items) {
            try {
                Map<String, Object> stockData = stockPriceService.fetchStockData(item.getSymbol());

                item.setCurrentPrice((BigDecimal) stockData.get("currentPrice"));
                item.setChangeAmount((BigDecimal) stockData.get("changeAmount"));
                item.setChangePercentage((BigDecimal) stockData.get("changePercentage"));

                if (item.shouldTriggerAlert()) {
                    item.setAlertTriggered(true);
                    log.info("Alert triggered for {}", item.getSymbol());
                }

            } catch (Exception e) {
                log.error("Price refresh failed for {}: {}",
                        item.getSymbol(), e.getMessage());
                // Continue with other items even if one fails
            }
        }

        wishlistRepository.saveAll(items);

        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Wishlist summary
     */
    @Transactional(readOnly = true)
    public WishlistSummaryDTO getWishlistSummary(Long portfolioId) {

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "id", portfolioId));

        WishlistSummaryDTO summary = new WishlistSummaryDTO();
        summary.setTotalWatchlist(wishlistRepository.countByPortfolio(portfolio));
        summary.setGainersCount(wishlistRepository.countGainers(portfolio));
        summary.setLosersCount(wishlistRepository.countLosers(portfolio));
        summary.setAlertsCount(
                wishlistRepository.countActiveAlerts(portfolio));

        return summary;
    }

    /**
     * ===== Helpers =====
     */
    private WishlistItem getValidatedItem(Long portfolioId, Long itemId) {

        WishlistItem item = wishlistRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item", "id", itemId));

        if (!item.getPortfolio().getId().equals(portfolioId)) {
            throw new UnauthorizedAccessException("Wishlist item", itemId);
        }

        return item;
    }

    /**
     * Entity → DTO
     */
    private WishlistItemDTO convertToDTO(WishlistItem item) {

        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setId(item.getId());
        dto.setSymbol(item.getSymbol());
        dto.setName(item.getName());
        dto.setCategory(item.getCategory());
        dto.setTargetPrice(item.getTargetPrice());
        dto.setPriceWhenAdded(item.getPriceWhenAdded());
        dto.setCurrentPrice(item.getCurrentPrice());
        dto.setChangeAmount(item.getChangeAmount());
        dto.setChangePercentage(item.getChangePercentage());
        dto.setPerformanceSinceAdded(item.getPerformanceSinceAdded());
        dto.setAlertEnabled(item.getAlertEnabled());
        dto.setAlertTriggered(item.getAlertTriggered());
        dto.setAddedAt(item.getAddedAt());
        dto.setLastUpdated(item.getLastUpdated());
        dto.setCurrency(item.getCurrency());

        return dto;
    }
}
