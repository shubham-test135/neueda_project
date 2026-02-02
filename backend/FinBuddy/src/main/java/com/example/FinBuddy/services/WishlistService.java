package com.example.FinBuddy.services;

import com.example.FinBuddy.dto.WishlistCreateDTO;
import com.example.FinBuddy.dto.WishlistDTO;
import com.example.FinBuddy.dto.WishlistUpdateDTO;
import com.example.FinBuddy.entities.User;
import com.example.FinBuddy.entities.Wishlist;
import com.example.FinBuddy.exception.ResourceAlreadyExistsException;
import com.example.FinBuddy.exception.ResourceNotFoundException;
import com.example.FinBuddy.repositories.UserRepository;
import com.example.FinBuddy.repositories.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing user's stock wishlist
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final StockMarketService stockMarketService;

    /**
     * Add a stock to user's wishlist
     */
    @Transactional
    public WishlistDTO addToWishlist(Long userId, WishlistCreateDTO createDTO) {
        log.info("Adding stock {} to wishlist for user {}", createDTO.getSymbol(), userId);

        String symbol = createDTO.getSymbol().toUpperCase();

        // Check if already exists
        if (wishlistRepository.existsByUserIdAndSymbolAndActiveTrue(userId, symbol)) {
            throw new ResourceAlreadyExistsException("Stock " + symbol + " already in wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Fetch stock details from Finnhub
        Map<String, Object> stockQuote = stockMarketService.getStockQuote(symbol);
        Map<String, Object> stockProfile = stockMarketService.getCompanyProfile(symbol);

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setSymbol(symbol);

        // Set stock details from API
        if (stockQuote != null && !stockQuote.isEmpty()) {
            wishlist.setCurrentPrice(getBigDecimal(stockQuote.get("c")));
            wishlist.setPreviousClose(getBigDecimal(stockQuote.get("pc")));
            wishlist.setDayHigh(getBigDecimal(stockQuote.get("h")));
            wishlist.setDayLow(getBigDecimal(stockQuote.get("l")));
            wishlist.setOpenPrice(getBigDecimal(stockQuote.get("o")));
            wishlist.setLastPriceUpdate(LocalDateTime.now());
        }

        if (stockProfile != null && !stockProfile.isEmpty()) {
            wishlist.setName((String) stockProfile.getOrDefault("name", symbol));
            wishlist.setExchange((String) stockProfile.get("exchange"));
            wishlist.setCurrency((String) stockProfile.getOrDefault("currency", "USD"));
            wishlist.setSector((String) stockProfile.get("finnhubIndustry"));
            wishlist.setIndustry((String) stockProfile.get("finnhubIndustry"));

            if (stockProfile.containsKey("marketCapitalization")) {
                wishlist.setMarketCap(formatMarketCap(stockProfile.get("marketCapitalization")));
            }
        }

        // Determine type based on symbol or profile
        wishlist.setType(determineSecurityType(symbol, stockProfile));

        // Set user preferences
        wishlist.setTargetPrice(createDTO.getTargetPrice());
        wishlist.setNotes(createDTO.getNotes());
        wishlist.setPriceAlertEnabled(createDTO.getPriceAlertEnabled() != null ?
                createDTO.getPriceAlertEnabled() : false);
        wishlist.setPriority(createDTO.getPriority() != null ? createDTO.getPriority() : 3);

        Wishlist saved = wishlistRepository.save(wishlist);
        log.info("Stock {} added to wishlist with ID {}", saved.getSymbol(), saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Get all wishlist items for a user
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getUserWishlist(Long userId) {
        log.info("Fetching wishlist for user {}", userId);
        List<Wishlist> wishlistItems = wishlistRepository.findByUserIdAndActiveTrueOrderByAddedAtDesc(userId);
        return wishlistItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get wishlist items sorted by priority
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getUserWishlistByPriority(Long userId) {
        log.info("Fetching wishlist by priority for user {}", userId);
        List<Wishlist> wishlistItems = wishlistRepository.findByUserIdAndActiveTrueOrderByPriorityAsc(userId);
        return wishlistItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get high priority wishlist items
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getHighPriorityItems(Long userId) {
        log.info("Fetching high priority items for user {}", userId);
        List<Wishlist> items = wishlistRepository.findHighPriorityItems(userId);
        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific wishlist item
     */
    @Transactional(readOnly = true)
    public WishlistDTO getWishlistItem(Long userId, Long wishlistId) {
        Wishlist wishlist = wishlistRepository.findByIdAndUserIdAndActiveTrue(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + wishlistId));

        return convertToDTO(wishlist);
    }

    /**
     * Update wishlist item
     */
    @Transactional
    public WishlistDTO updateWishlistItem(Long userId, Long wishlistId, WishlistUpdateDTO updateDTO) {
        log.info("Updating wishlist item {} for user {}", wishlistId, userId);

        Wishlist wishlist = wishlistRepository.findByIdAndUserIdAndActiveTrue(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + wishlistId));

        if (updateDTO.getTargetPrice() != null) {
            wishlist.setTargetPrice(updateDTO.getTargetPrice());
        }
        if (updateDTO.getNotes() != null) {
            wishlist.setNotes(updateDTO.getNotes());
        }
        if (updateDTO.getPriceAlertEnabled() != null) {
            wishlist.setPriceAlertEnabled(updateDTO.getPriceAlertEnabled());
        }
        if (updateDTO.getPriority() != null) {
            wishlist.setPriority(updateDTO.getPriority());
        }

        Wishlist updated = wishlistRepository.save(wishlist);
        log.info("Wishlist item {} updated successfully", wishlistId);

        return convertToDTO(updated);
    }

    /**
     * Remove item from wishlist (soft delete)
     */
    @Transactional
    public void removeFromWishlist(Long userId, Long wishlistId) {
        log.info("Removing wishlist item {} for user {}", wishlistId, userId);

        Wishlist wishlist = wishlistRepository.findByIdAndUserIdAndActiveTrue(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + wishlistId));

        wishlist.setActive(false);
        wishlistRepository.save(wishlist);
        log.info("Wishlist item {} removed successfully", wishlistId);
    }

    /**
     * Refresh prices for a specific wishlist item
     */
    @Transactional
    public WishlistDTO refreshItemPrice(Long userId, Long wishlistId) {
        log.info("Refreshing price for wishlist item {} for user {}", wishlistId, userId);

        Wishlist item = wishlistRepository.findByIdAndUserIdAndActiveTrue(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + wishlistId));

        updateItemPrice(item);
        Wishlist updated = wishlistRepository.save(item);

        return convertToDTO(updated);
    }

    /**
     * Refresh prices for all wishlist items of a user
     */
    @Transactional
    public void refreshAllPrices(Long userId) {
        log.info("Refreshing prices for all wishlist items of user {}", userId);

        List<Wishlist> wishlistItems = wishlistRepository.findByUserIdAndActiveTrue(userId);
        int updatedCount = 0;

        for (Wishlist item : wishlistItems) {
            try {
                updateItemPrice(item);
                updatedCount++;
            } catch (Exception e) {
                log.error("Error refreshing price for symbol {}: {}", item.getSymbol(), e.getMessage());
            }
        }

        wishlistRepository.saveAll(wishlistItems);
        log.info("Refreshed prices for {}/{} items", updatedCount, wishlistItems.size());
    }

    /**
     * Get items where target price is met
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getTargetPriceMetItems(Long userId) {
        log.info("Fetching items where target price is met for user {}", userId);
        List<Wishlist> items = wishlistRepository.findTargetPriceMetItems(userId);
        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get items with price alerts enabled
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getPriceAlertEnabledItems(Long userId) {
        log.info("Fetching price alert enabled items for user {}", userId);
        List<Wishlist> items = wishlistRepository.findByUserIdAndPriceAlertEnabled(userId);
        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get items by type (Stock, ETF, Bond, etc.)
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getItemsByType(Long userId, String type) {
        log.info("Fetching {} items for user {}", type, userId);
        List<Wishlist> items = wishlistRepository.findByUserIdAndType(userId, type);
        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get items by sector
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getItemsBySector(Long userId, String sector) {
        log.info("Fetching items in sector {} for user {}", sector, userId);
        List<Wishlist> items = wishlistRepository.findByUserIdAndSector(userId, sector);
        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search wishlist
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> searchWishlist(Long userId, String searchTerm) {
        log.info("Searching wishlist for user {} with term: {}", userId, searchTerm);
        List<Wishlist> items = wishlistRepository.searchWishlist(userId, searchTerm);
        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get top performers
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getTopPerformers(Long userId, int limit) {
        log.info("Fetching top {} performers for user {}", limit, userId);
        List<Wishlist> items = wishlistRepository.findTopPerformers(userId);
        return items.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get worst performers
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getWorstPerformers(Long userId, int limit) {
        log.info("Fetching worst {} performers for user {}", limit, userId);
        List<Wishlist> items = wishlistRepository.findWorstPerformers(userId);
        return items.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get wishlist statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWishlistStatistics(Long userId) {
        log.info("Fetching wishlist statistics for user {}", userId);

        Long totalCount = wishlistRepository.countByUserIdAndActiveTrue(userId);
        Long targetMetCount = wishlistRepository.countTargetPriceMetByUserId(userId);
        Long stocksCount = wishlistRepository.countByUserIdAndType(userId, "Common Stock");
        Long etfsCount = wishlistRepository.countByUserIdAndType(userId, "ETF");
        Long bondsCount = wishlistRepository.countByUserIdAndType(userId, "Bond");
        BigDecimal avgPrice = wishlistRepository.getAverageCurrentPriceByUserId(userId);

        List<String> sectors = wishlistRepository.findDistinctSectorsByUserId(userId);

        return Map.of(
                "totalItems", totalCount,
                "targetPriceMet", targetMetCount,
                "stocks", stocksCount,
                "etfs", etfsCount,
                "bonds", bondsCount,
                "averagePrice", avgPrice != null ? avgPrice : BigDecimal.ZERO,
                "sectors", sectors
        );
    }

    /**
     * Scheduled job to refresh prices every 15 minutes during market hours
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Async
    @Transactional
    public void scheduledPriceRefresh() {
        log.info("Starting scheduled price refresh for all active wishlist items");

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Wishlist> itemsToUpdate = wishlistRepository.findItemsNeedingPriceUpdate(oneHourAgo);

        log.info("Found {} items needing price update", itemsToUpdate.size());

        int updatedCount = 0;
        for (Wishlist item : itemsToUpdate) {
            try {
                updateItemPrice(item);
                updatedCount++;

                // Rate limiting - sleep for 100ms between API calls
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Error in scheduled price refresh for {}: {}", item.getSymbol(), e.getMessage());
            }
        }

        wishlistRepository.saveAll(itemsToUpdate);
        log.info("Scheduled price refresh completed. Updated {}/{} items", updatedCount, itemsToUpdate.size());
    }

    /**
     * Check for price alerts
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Async
    @Transactional
    public void checkPriceAlerts() {
        log.info("Checking for price alerts");

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Wishlist> items = wishlistRepository.findItemsNeedingAlert(oneHourAgo);

        log.info("Found {} items with target price met", items.size());

        for (Wishlist item : items) {
            try {
                // Send alert (implement email/notification service here)
                sendPriceAlert(item);

                // Update last alert sent timestamp
                wishlistRepository.updateLastAlertSent(item.getId(), LocalDateTime.now());
                log.info("Price alert sent for {} ({})", item.getSymbol(), item.getUser().getEmail());
            } catch (Exception e) {
                log.error("Error sending alert for {}: {}", item.getSymbol(), e.getMessage());
            }
        }
    }

    // Helper methods

    private void updateItemPrice(Wishlist item) {
        try {
            Map<String, Object> stockQuote = stockMarketService.getStockQuote(item.getSymbol());

            if (stockQuote != null && stockQuote.containsKey("c")) {
                item.setCurrentPrice(getBigDecimal(stockQuote.get("c")));
                item.setPreviousClose(getBigDecimal(stockQuote.get("pc")));
                item.setDayHigh(getBigDecimal(stockQuote.get("h")));
                item.setDayLow(getBigDecimal(stockQuote.get("l")));
                item.setOpenPrice(getBigDecimal(stockQuote.get("o")));
                item.setLastPriceUpdate(LocalDateTime.now());

                // Check if target price is met
                if (item.getTargetPrice() != null && item.getCurrentPrice() != null) {
                    item.setTargetPriceMet(item.getCurrentPrice().compareTo(item.getTargetPrice()) <= 0);
                }

                log.debug("Updated price for {}: {}", item.getSymbol(), item.getCurrentPrice());
            }
        } catch (Exception e) {
            log.error("Error updating price for {}: {}", item.getSymbol(), e.getMessage());
            throw e;
        }
    }

    private void sendPriceAlert(Wishlist item) {
        // TODO: Implement email/notification service integration
        log.info("ALERT: {} ({}) has reached target price. Current: {}, Target: {}",
                item.getSymbol(),
                item.getName(),
                item.getCurrentPrice(),
                item.getTargetPrice());
    }

    private WishlistDTO convertToDTO(Wishlist wishlist) {
        WishlistDTO dto = WishlistDTO.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .symbol(wishlist.getSymbol())
                .name(wishlist.getName())
                .exchange(wishlist.getExchange())
                .type(wishlist.getType())
                .currency(wishlist.getCurrency())
                .targetPrice(wishlist.getTargetPrice())
                .currentPrice(wishlist.getCurrentPrice())
                .previousClose(wishlist.getPreviousClose())
                .dayHigh(wishlist.getDayHigh())
                .dayLow(wishlist.getDayLow())
                .openPrice(wishlist.getOpenPrice())
                .notes(wishlist.getNotes())
                .priceAlertEnabled(wishlist.getPriceAlertEnabled())
                .targetPriceMet(wishlist.getTargetPriceMet())
                .priority(wishlist.getPriority())
                .sector(wishlist.getSector())
                .industry(wishlist.getIndustry())
                .marketCap(wishlist.getMarketCap())
                .addedAt(wishlist.getAddedAt())
                .updatedAt(wishlist.getUpdatedAt())
                .lastPriceUpdate(wishlist.getLastPriceUpdate())
                .lastAlertSent(wishlist.getLastAlertSent())
                .active(wishlist.getActive())
                .build();

        // Calculate derived fields
        if (wishlist.getCurrentPrice() != null && wishlist.getPreviousClose() != null) {
            dto.setPriceChange(wishlist.getPriceChange());
            dto.setPriceChangePercentage(wishlist.getPriceChangePercentage());
        }

        if (wishlist.getTargetPrice() != null && wishlist.getCurrentPrice() != null) {
            dto.setDistanceToTarget(wishlist.getDistanceToTarget());
            dto.setDistanceToTargetPercentage(wishlist.getDistanceToTargetPercentage());

            // Set price status
            int comparison = wishlist.getCurrentPrice().compareTo(wishlist.getTargetPrice());
            if (comparison < 0) {
                dto.setPriceStatus("Below Target");
            } else if (comparison == 0) {
                dto.setPriceStatus("At Target");
            } else {
                dto.setPriceStatus("Above Target");
            }
        }

        // Set priority label
        dto.setPriorityLabel(getPriorityLabel(wishlist.getPriority()));

        return dto;
    }

    private String getPriorityLabel(Integer priority) {
        if (priority == null) return "Low";
        switch (priority) {
            case 1: return "High";
            case 2: return "Medium";
            case 3: return "Low";
            default: return "Unknown";
        }
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

    private String determineSecurityType(String symbol, Map<String, Object> profile) {
        // Check if it's an ETF or bond based on symbol patterns
        if (symbol.matches(".*[A-Z]{3,4}")) {
            if (symbol.endsWith("X") || symbol.contains("ETF")) {
                return "ETF";
            }
        }

        // Default to Common Stock
        return "Common Stock";
    }
}