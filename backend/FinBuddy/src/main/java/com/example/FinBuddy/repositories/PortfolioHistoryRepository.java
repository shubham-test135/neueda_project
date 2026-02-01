package com.example.FinBuddy.repositories;

import com.example.FinBuddy.entities.PortfolioHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Portfolio History entity
 */
@Repository
public interface PortfolioHistoryRepository extends JpaRepository<PortfolioHistory, Long> {

    /**
     * Find history by portfolio ID ordered by date
     */
    List<PortfolioHistory> findByPortfolioIdOrderByRecordDateAsc(Long portfolioId);

    /**
     * Find history within date range
     */
    List<PortfolioHistory> findByPortfolioIdAndRecordDateBetweenOrderByRecordDateAsc(
            Long portfolioId, LocalDate startDate, LocalDate endDate);

    /**
     * Delete old history records before a certain date
     */
    void deleteByPortfolioIdAndRecordDateBefore(Long portfolioId, LocalDate beforeDate);
}
