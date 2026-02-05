package com.example.FinBuddy.repositories;

import com.example.FinBuddy.entities.Asset;
import com.example.FinBuddy.entities.Bond;
import com.example.FinBuddy.entities.MutualFund;
import com.example.FinBuddy.entities.SIP;
import com.example.FinBuddy.entities.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Asset entity
 */
@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * Find all assets by portfolio ID
     */
    List<Asset> findByPortfolioId(Long portfolioId);

    /**
     * Search assets by name or symbol (case-insensitive)
     * Supports all asset types: Stock, Bond, MutualFund, SIP
     */
    @Query("SELECT a FROM Asset a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(a.symbol) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Asset> searchByNameOrSymbol(@Param("searchTerm") String searchTerm);

    /**
     * Search stocks by name or symbol
     */
    @Query("SELECT a FROM Stock a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(a.symbol) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Asset> searchStocks(@Param("searchTerm") String searchTerm);

    /**
     * Search bonds by name or symbol
     */
    @Query("SELECT a FROM Bond a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(a.symbol) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Asset> searchBonds(@Param("searchTerm") String searchTerm);

    /**
     * Search mutual funds by name or symbol
     */
    @Query("SELECT a FROM MutualFund a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(a.symbol) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Asset> searchMutualFunds(@Param("searchTerm") String searchTerm);

    /**
     * Search SIPs by name or symbol
     */
    @Query("SELECT a FROM SIP a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(a.symbol) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Asset> searchSIPs(@Param("searchTerm") String searchTerm);

    /**
     * Find assets by type
     */
    @Query("SELECT a FROM Asset a WHERE TYPE(a) = :assetClass")
    List<Asset> findByAssetType(@Param("assetClass") Class<? extends Asset> assetClass);

    /**
     * Find wishlist assets
     */
    List<Asset> findByIsWishlistTrue();

    /**
     * Find wishlist assets by portfolio
     */
    List<Asset> findByPortfolioIdAndIsWishlistTrue(Long portfolioId);

    /**
     * Find actual invested assets (non-wishlist)
     */
    List<Asset> findByPortfolioIdAndIsWishlistFalse(Long portfolioId);

    /**
     * Find assets by symbol
     */
    List<Asset> findBySymbol(String symbol);
}
