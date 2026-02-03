package com.example.FinBuddy.repositories;

import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.entities.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    /**
     * Get wishlist for a portfolio
     */
    List<WishlistItem> findByPortfolioOrderByAddedAtDesc(Portfolio portfolio);

    /**
     * Find wishlist item by symbol in portfolio
     */
    Optional<WishlistItem> findByPortfolioAndSymbol(Portfolio portfolio, String symbol);

    /**
     * Check if symbol already exists in wishlist
     */
    boolean existsByPortfolioAndSymbol(Portfolio portfolio, String symbol);

    /**
     * Filter by category
     */
    List<WishlistItem> findByPortfolioAndCategory(Portfolio portfolio, String category);

    /**
     * Active alerts (list)
     */
    List<WishlistItem> findByPortfolioAndAlertEnabledTrue(Portfolio portfolio);

    /**
     * Triggered alerts
     */
    @Query("""
        SELECT w
        FROM WishlistItem w
        WHERE w.portfolio = :portfolio
          AND w.alertEnabled = true
          AND w.alertTriggered = false
          AND w.currentPrice >= w.targetPrice
    """)
    List<WishlistItem> findTriggeredAlerts(@Param("portfolio") Portfolio portfolio);

    /**
     * Count wishlist items
     */
    long countByPortfolio(Portfolio portfolio);

    /**
     * Count gainers
     */
    @Query("""
        SELECT COUNT(w)
        FROM WishlistItem w
        WHERE w.portfolio = :portfolio
          AND w.changePercentage > 0
    """)
    long countGainers(@Param("portfolio") Portfolio portfolio);

    /**
     * Count losers
     */
    @Query("""
        SELECT COUNT(w)
        FROM WishlistItem w
        WHERE w.portfolio = :portfolio
          AND w.changePercentage < 0
    """)
    long countLosers(@Param("portfolio") Portfolio portfolio);

    /**
     * ✅ Count active alerts (THIS WAS MISSING)
     */
    @Query("""
        SELECT COUNT(w)
        FROM WishlistItem w
        WHERE w.portfolio = :portfolio
          AND w.alertEnabled = true
    """)
    long countActiveAlerts(@Param("portfolio") Portfolio portfolio);

    /**
     * Delete by symbol
     */
    void deleteByPortfolioAndSymbol(Portfolio portfolio, String symbol);
}
