package com.example.FinBuddy.repositories;

import com.example.FinBuddy.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Portfolio entity
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Find portfolio by name
     */
    Optional<Portfolio> findByName(String name);

    /**
     * Find all portfolios ordered by creation date
     */
    List<Portfolio> findAllByOrderByCreatedAtDesc();

    /**
     * Find portfolios by base currency
     */
    List<Portfolio> findByBaseCurrency(String baseCurrency);

    /**
     * Get portfolio with all assets loaded
     */
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.assets WHERE p.id = :id")
    Optional<Portfolio> findByIdWithAssets(Long id);
}
