package com.example.FinBuddy.controllers;

import com.example.FinBuddy.dto.DashboardSummaryDTO;
import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.entities.PortfolioHistory;
import com.example.FinBuddy.services.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Portfolio management
 */
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Get all portfolios
     */
    @GetMapping
    public ResponseEntity<List<Portfolio>> getAllPortfolios() {
        List<Portfolio> portfolios = portfolioService.getAllPortfolios();
        return ResponseEntity.ok(portfolios);
    }

    /**
     * Get portfolio by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getPortfolioById(@PathVariable Long id) {
        return portfolioService.getPortfolioById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get portfolio with all assets
     */
    @GetMapping("/{id}/with-assets")
    public ResponseEntity<Portfolio> getPortfolioWithAssets(@PathVariable Long id) {
        return portfolioService.getPortfolioWithAssets(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new portfolio
     */
    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody Portfolio portfolio) {
        Portfolio created = portfolioService.createPortfolio(portfolio);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update portfolio
     */
    @PutMapping("/{id}")
    public ResponseEntity<Portfolio> updatePortfolio(
            @PathVariable Long id,
            @RequestBody Portfolio portfolio) {
        try {
            Portfolio updated = portfolioService.updatePortfolio(id, portfolio);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete portfolio
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Recalculate portfolio metrics
     */
    @PostMapping("/{id}/recalculate")
    public ResponseEntity<Portfolio> recalculateMetrics(@PathVariable Long id) {
        try {
            Portfolio portfolio = portfolioService.recalculatePortfolioMetrics(id);
            return ResponseEntity.ok(portfolio);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get dashboard summary
     */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(@PathVariable Long id) {
        try {
            DashboardSummaryDTO dashboard = portfolioService.getDashboardSummary(id);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get portfolio performance history
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<PortfolioHistory>> getPortfolioHistory(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<PortfolioHistory> history = portfolioService.getPortfolioHistory(id, startDate, endDate);
        return ResponseEntity.ok(history);
    }
}
