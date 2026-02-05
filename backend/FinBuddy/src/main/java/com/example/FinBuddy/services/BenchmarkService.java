package com.example.FinBuddy.services;

import com.example.FinBuddy.dto.BenchmarkRequest;
import com.example.FinBuddy.entities.Benchmark;
import com.example.FinBuddy.entities.Portfolio;
import com.example.FinBuddy.exceptions.DuplicateResourceException;
import com.example.FinBuddy.exceptions.ResourceNotFoundException;
import com.example.FinBuddy.repositories.BenchmarkRepository;
import com.example.FinBuddy.repositories.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service for managing portfolio benchmarks
 */
@Service
public class BenchmarkService {

    private static final Logger logger = LoggerFactory.getLogger(BenchmarkService.class);

    @Autowired
    private BenchmarkRepository benchmarkRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private StockPriceService stockPriceService;

    /**
     * Add a benchmark to a portfolio
     */
    @Transactional
    public Benchmark addBenchmark(Long portfolioId, BenchmarkRequest request) {
        logger.info("Adding benchmark {} to portfolio {}", request.getSymbol(), portfolioId);

        // Validate portfolio exists
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with ID: " + portfolioId));

        // Check for duplicates
        if (benchmarkRepository.existsByPortfolioIdAndSymbol(portfolioId, request.getSymbol())) {
            throw new DuplicateResourceException(
                    "Benchmark " + request.getSymbol() + " already exists for this portfolio");
        }

        // Create benchmark
        Benchmark benchmark = new Benchmark(portfolio, request.getSymbol(), request.getName());
        benchmark.setIndexType(request.getIndexType());
        benchmark.setDescription(request.getDescription());
        benchmark.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");

        // Fetch current value from market data
        try {
            Map<String, Object> marketData = stockPriceService.getBenchmarkWithChange(request.getSymbol());
            if (marketData.get("value") != null) {
                benchmark.setCurrentValue((BigDecimal) marketData.get("value"));
            }
            if (marketData.get("change") != null) {
                benchmark.setChangeAmount((BigDecimal) marketData.get("change"));
            }
            if (marketData.get("changePercent") != null) {
                benchmark.setChangePercentage((BigDecimal) marketData.get("changePercent"));
            }
        } catch (Exception e) {
            logger.warn("Could not fetch market data for benchmark {}: {}", request.getSymbol(), e.getMessage());
            // Continue with null values - can be updated later
        }

        Benchmark saved = benchmarkRepository.save(benchmark);
        logger.info("Benchmark {} added successfully with ID: {}", request.getSymbol(), saved.getId());
        return saved;
    }

    /**
     * Get all benchmarks for a portfolio
     */
    public List<Benchmark> getBenchmarks(Long portfolioId) {
        logger.info("Fetching benchmarks for portfolio {}", portfolioId);

        // Validate portfolio exists
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new ResourceNotFoundException("Portfolio not found with ID: " + portfolioId);
        }

        return benchmarkRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Delete a benchmark
     */
    @Transactional
    public void deleteBenchmark(Long portfolioId, Long benchmarkId) {
        logger.info("Deleting benchmark {} from portfolio {}", benchmarkId, portfolioId);

        Benchmark benchmark = benchmarkRepository.findById(benchmarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Benchmark not found with ID: " + benchmarkId));

        // Verify benchmark belongs to the portfolio
        if (!benchmark.getPortfolio().getId().equals(portfolioId)) {
            throw new ResourceNotFoundException("Benchmark " + benchmarkId + " not found in portfolio " + portfolioId);
        }

        benchmarkRepository.delete(benchmark);
        logger.info("Benchmark {} deleted successfully", benchmarkId);
    }

    /**
     * Refresh all benchmark values for a portfolio
     */
    @Transactional
    public List<Benchmark> refreshBenchmarks(Long portfolioId) {
        logger.info("Refreshing benchmarks for portfolio {}", portfolioId);

        List<Benchmark> benchmarks = getBenchmarks(portfolioId);

        for (Benchmark benchmark : benchmarks) {
            try {
                Map<String, Object> marketData = stockPriceService.getBenchmarkWithChange(benchmark.getSymbol());

                if (marketData.get("value") != null) {
                    benchmark.setCurrentValue((BigDecimal) marketData.get("value"));
                }
                if (marketData.get("change") != null) {
                    benchmark.setChangeAmount((BigDecimal) marketData.get("change"));
                }
                if (marketData.get("changePercent") != null) {
                    benchmark.setChangePercentage((BigDecimal) marketData.get("changePercent"));
                }

                benchmarkRepository.save(benchmark);
                logger.debug("Refreshed benchmark {}", benchmark.getSymbol());
            } catch (Exception e) {
                logger.error("Failed to refresh benchmark {}: {}", benchmark.getSymbol(), e.getMessage());
                // Continue with other benchmarks
            }
        }

        logger.info("Benchmark refresh completed for portfolio {}", portfolioId);
        return benchmarks;
    }
}
