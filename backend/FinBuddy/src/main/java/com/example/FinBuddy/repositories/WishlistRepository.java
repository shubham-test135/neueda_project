package com.example.FinBuddy.repositories;

import com.example.FinBuddy.entities.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Wishlist entity operations
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // Basic queries
    List<Wishlist> findByUserIdAndActiveTrue(Long userId);

    List<Wishlist> findByUserIdAndActiveTrueOrderByAddedAtDesc(Long userId);

    List<Wishlist> findByUserIdAndActiveTrueOrderByPriorityAsc(Long userId);

    Optional<Wishlist> findByIdAndUserIdAndActiveTrue(Long id, Long userId);

    Optional<Wishlist> findByUserIdAndSymbolAndActiveTrue(Long userId, String symbol);

    boolean existsByUserIdAndSymbolAndActiveTrue(Long userId, String symbol);

    Long countByUserIdAndActiveTrue(Long userId);

    // Priority-based queries
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.priority = :priority ORDER BY w.addedAt DESC")
    List<Wishlist> findByUserIdAndPriority(@Param("userId") Long userId, @Param("priority") Integer priority);

    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.priority = 1 ORDER BY w.addedAt DESC")
    List<Wishlist> findHighPriorityItems(@Param("userId") Long userId);

    // Price alert queries
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.priceAlertEnabled = true")
    List<Wishlist> findByUserIdAndPriceAlertEnabled(@Param("userId") Long userId);

    @Query("SELECT w FROM Wishlist w WHERE w.active = true AND w.priceAlertEnabled = true")
    List<Wishlist> findAllWithPriceAlertEnabled();

    // Target price queries
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.currentPrice <= w.targetPrice AND w.targetPrice IS NOT NULL")
    List<Wishlist> findTargetPriceMetItems(@Param("userId") Long userId);

    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.targetPriceMet = true")
    List<Wishlist> findByUserIdAndTargetPriceMet(@Param("userId") Long userId);

    @Query("SELECT w FROM Wishlist w WHERE w.active = true AND w.priceAlertEnabled = true " +
            "AND w.currentPrice <= w.targetPrice AND w.targetPrice IS NOT NULL " +
            "AND (w.lastAlertSent IS NULL OR w.lastAlertSent < :since)")
    List<Wishlist> findItemsNeedingAlert(@Param("since") LocalDateTime since);

    // Type-based queries
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.type = :type")
    List<Wishlist> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.type IN :types")
    List<Wishlist> findByUserIdAndTypes(@Param("userId") Long userId, @Param("types") List<String> types);

    // Sector/Industry queries
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.sector = :sector")
    List<Wishlist> findByUserIdAndSector(@Param("userId") Long userId, @Param("sector") String sector);

    @Query("SELECT DISTINCT w.sector FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.sector IS NOT NULL")
    List<String> findDistinctSectorsByUserId(@Param("userId") Long userId);

    // Price update queries
    @Query("SELECT w FROM Wishlist w WHERE w.active = true AND " +
            "(w.lastPriceUpdate IS NULL OR w.lastPriceUpdate < :before)")
    List<Wishlist> findItemsNeedingPriceUpdate(@Param("before") LocalDateTime before);

    // Search queries
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND " +
            "(LOWER(w.symbol) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Wishlist> searchWishlist(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);

    // Statistics queries
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.type = :type")
    Long countByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.targetPriceMet = true")
    Long countTargetPriceMetByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(w.currentPrice) FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.currentPrice IS NOT NULL")
    BigDecimal getAverageCurrentPriceByUserId(@Param("userId") Long userId);

    // Bulk operations
    @Modifying
    @Query("UPDATE Wishlist w SET w.active = false WHERE w.user.id = :userId")
    int softDeleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Wishlist w SET w.lastAlertSent = :timestamp WHERE w.id = :id")
    int updateLastAlertSent(@Param("id") Long id, @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE Wishlist w SET w.targetPriceMet = true WHERE w.id = :id")
    int markTargetPriceMet(@Param("id") Long id);

    // Date range queries
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true AND w.addedAt BETWEEN :startDate AND :endDate")
    List<Wishlist> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Top performers query
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true " +
            "AND w.currentPrice IS NOT NULL AND w.previousClose IS NOT NULL " +
            "ORDER BY ((w.currentPrice - w.previousClose) / w.previousClose) DESC")
    List<Wishlist> findTopPerformers(@Param("userId") Long userId);

    // Bottom performers query
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.active = true " +
            "AND w.currentPrice IS NOT NULL AND w.previousClose IS NOT NULL " +
            "ORDER BY ((w.currentPrice - w.previousClose) / w.previousClose) ASC")
    List<Wishlist> findWorstPerformers(@Param("userId") Long userId);
}