package com.example.FinBuddy.services;

import com.example.FinBuddy.entities.*;
import com.example.FinBuddy.repositories.AssetRepository;
import com.example.FinBuddy.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Asset management
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AssetService {

    private final AssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioService portfolioService;

    /**
     * Create a new asset
     */
    public Asset createAsset(Asset asset, Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        asset.setPortfolio(portfolio);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        asset.calculateMetrics();

        Asset savedAsset = assetRepository.save(asset);

        // Recalculate portfolio metrics
        portfolioService.recalculatePortfolioMetrics(portfolioId);

        return savedAsset;
    }

    /**
     * Get all assets
     */
    @Transactional(readOnly = true)
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    /**
     * Get asset by ID
     */
    @Transactional(readOnly = true)
    public Optional<Asset> getAssetById(Long id) {
        return assetRepository.findById(id);
    }

    /**
     * Get all assets for a portfolio
     */
    @Transactional(readOnly = true)
    public List<Asset> getAssetsByPortfolio(Long portfolioId) {
        return assetRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Get invested assets (non-wishlist) for a portfolio
     */
    @Transactional(readOnly = true)
    public List<Asset> getInvestedAssets(Long portfolioId) {
        return assetRepository.findByPortfolioIdAndIsWishlistFalse(portfolioId);
    }

    /**
     * Get wishlist assets for a portfolio
     */
    @Transactional(readOnly = true)
    public List<Asset> getWishlistAssets(Long portfolioId) {
        return assetRepository.findByPortfolioIdAndIsWishlistTrue(portfolioId);
    }

    /**
     * Search assets by name or symbol
     * Searches across all asset types: Stocks, Bonds, Mutual Funds, and SIPs
     */
    @Transactional(readOnly = true)
    public List<Asset> searchAssets(String searchTerm) {
        return assetRepository.searchByNameOrSymbol(searchTerm);
    }

    /**
     * Search only stocks
     */
    @Transactional(readOnly = true)
    public List<Asset> searchStocks(String searchTerm) {
        return assetRepository.searchStocks(searchTerm);
    }

    /**
     * Search only bonds
     */
    @Transactional(readOnly = true)
    public List<Asset> searchBonds(String searchTerm) {
        return assetRepository.searchBonds(searchTerm);
    }

    /**
     * Search only mutual funds
     */
    @Transactional(readOnly = true)
    public List<Asset> searchMutualFunds(String searchTerm) {
        return assetRepository.searchMutualFunds(searchTerm);
    }

    /**
     * Search only SIPs
     */
    @Transactional(readOnly = true)
    public List<Asset> searchSIPs(String searchTerm) {
        return assetRepository.searchSIPs(searchTerm);
    }

    /**
     * Update asset
     */
    public Asset updateAsset(Long id, Asset assetDetails) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        asset.setName(assetDetails.getName());
        asset.setSymbol(assetDetails.getSymbol());
        asset.setQuantity(assetDetails.getQuantity());
        asset.setPurchasePrice(assetDetails.getPurchasePrice());
        asset.setCurrentPrice(assetDetails.getCurrentPrice());
        asset.setCurrency(assetDetails.getCurrency());
        asset.setPurchaseDate(assetDetails.getPurchaseDate());
        asset.setIsWishlist(assetDetails.getIsWishlist());
        asset.setUpdatedAt(LocalDateTime.now());

        // Update type-specific fields
        updateTypeSpecificFields(asset, assetDetails);

        asset.calculateMetrics();

        Asset savedAsset = assetRepository.save(asset);

        // Recalculate portfolio metrics
        portfolioService.recalculatePortfolioMetrics(asset.getPortfolio().getId());

        return savedAsset;
    }

    /**
     * Update type-specific fields based on asset type
     */
    private void updateTypeSpecificFields(Asset asset, Asset assetDetails) {
        if (asset instanceof Stock && assetDetails instanceof Stock) {
            Stock stock = (Stock) asset;
            Stock stockDetails = (Stock) assetDetails;
            stock.setExchange(stockDetails.getExchange());
            stock.setSector(stockDetails.getSector());
            stock.setMarketCap(stockDetails.getMarketCap());
            stock.setDividendYield(stockDetails.getDividendYield());
            stock.setPeRatio(stockDetails.getPeRatio());
        } else if (asset instanceof Bond && assetDetails instanceof Bond) {
            Bond bond = (Bond) asset;
            Bond bondDetails = (Bond) assetDetails;
            bond.setCouponRate(bondDetails.getCouponRate());
            bond.setMaturityDate(bondDetails.getMaturityDate());
            bond.setFaceValue(bondDetails.getFaceValue());
            bond.setBondType(bondDetails.getBondType());
            bond.setIssuer(bondDetails.getIssuer());
            bond.setCreditRating(bondDetails.getCreditRating());
        } else if (asset instanceof MutualFund && assetDetails instanceof MutualFund) {
            MutualFund fund = (MutualFund) asset;
            MutualFund fundDetails = (MutualFund) assetDetails;
            fund.setNav(fundDetails.getNav());
            fund.setFundType(fundDetails.getFundType());
            fund.setFundHouse(fundDetails.getFundHouse());
            fund.setAum(fundDetails.getAum());
            fund.setExpenseRatio(fundDetails.getExpenseRatio());
            fund.setRiskLevel(fundDetails.getRiskLevel());
            fund.setSchemeCode(fundDetails.getSchemeCode());
            fund.setCategory(fundDetails.getCategory());
        } else if (asset instanceof SIP && assetDetails instanceof SIP) {
            SIP sip = (SIP) asset;
            SIP sipDetails = (SIP) assetDetails;
            sip.setMonthlyInvestment(sipDetails.getMonthlyInvestment());
            sip.setStartDate(sipDetails.getStartDate());
            sip.setEndDate(sipDetails.getEndDate());
            sip.setFrequency(sipDetails.getFrequency());
            sip.setSchemeName(sipDetails.getSchemeName());
            sip.setFundHouse(sipDetails.getFundHouse());
            sip.setIsActive(sipDetails.getIsActive());
            sip.setTotalInstallments(sipDetails.getTotalInstallments());
        }
    }

    /**
     * Update asset current price (for real-time updates)
     */
    public Asset updateAssetPrice(Long id, java.math.BigDecimal newPrice) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        asset.setCurrentPrice(newPrice);
        asset.setUpdatedAt(LocalDateTime.now());
        asset.calculateMetrics();

        Asset savedAsset = assetRepository.save(asset);

        // Recalculate portfolio metrics
        portfolioService.recalculatePortfolioMetrics(asset.getPortfolio().getId());

        return savedAsset;
    }

    /**
     * Delete asset
     */
    public void deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        Long portfolioId = asset.getPortfolio().getId();
        assetRepository.deleteById(id);

        // Recalculate portfolio metrics
        portfolioService.recalculatePortfolioMetrics(portfolioId);
    }

    /**
     * Partially sell asset (reduce quantity)
     */
    public Asset sellAsset(Long id, Double quantityToSell) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        if (quantityToSell <= 0) {
            throw new RuntimeException("Quantity to sell must be greater than 0");
        }

        if (quantityToSell > asset.getQuantity()) {
            throw new RuntimeException("Cannot sell more than owned quantity");
        }

        // Reduce quantity - convert to Integer
        Integer newQuantity = asset.getQuantity() - quantityToSell.intValue();
        asset.setQuantity(newQuantity);
        asset.setUpdatedAt(LocalDateTime.now());
        asset.calculateMetrics();

        Asset savedAsset = assetRepository.save(asset);

        // Recalculate portfolio metrics
        portfolioService.recalculatePortfolioMetrics(asset.getPortfolio().getId());

        return savedAsset;
    }

    /**
     * Get assets by type
     */
    @Transactional(readOnly = true)
    public List<Asset> getAssetsByType(Class<? extends Asset> assetClass) {
        return assetRepository.findByAssetType(assetClass);
    }
}
