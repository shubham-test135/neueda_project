package com.example.FinBuddy.dto;

/**
 * DTO for adding/updating benchmark
 */
public class BenchmarkRequest {

    private String symbol;
    private String name;
    private String indexType;
    private String description;
    private String currency;

    // Constructors
    public BenchmarkRequest() {
    }

    public BenchmarkRequest(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
