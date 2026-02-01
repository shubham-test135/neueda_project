package com.example.FinBuddy.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Portfolio History entity for tracking portfolio value over time
 * Used for generating performance charts
 */
@Entity
@Table(name = "portfolio_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalValue;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalInvestment;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal gainLoss;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal gainLossPercentage;
}
