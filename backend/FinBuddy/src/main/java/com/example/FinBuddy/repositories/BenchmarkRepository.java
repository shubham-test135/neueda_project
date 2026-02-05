package com.example.FinBuddy.repositories;

import com.example.FinBuddy.entities.Benchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Benchmark entity operations
 */
@Repository
public interface BenchmarkRepository extends JpaRepository<Benchmark, Long> {

    /**
     * Find all benchmarks for a specific portfolio
     */
    List<Benchmark> findByPortfolioId(Long portfolioId);

    /**
     * Find a benchmark by portfolio ID and symbol
     */
    Optional<Benchmark> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    /**
     * Check if a benchmark exists for a portfolio with the given symbol
     */
    boolean existsByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    /**
     * Delete all benchmarks for a portfolio
     */
    void deleteByPortfolioId(Long portfolioId);

    /**
     * Count benchmarks for a portfolio
     */
    long countByPortfolioId(Long portfolioId);
}
