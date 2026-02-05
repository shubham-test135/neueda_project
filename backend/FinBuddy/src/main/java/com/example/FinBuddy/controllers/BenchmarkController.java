package com.example.FinBuddy.controllers;

import com.example.FinBuddy.dto.BenchmarkRequest;
import com.example.FinBuddy.entities.Benchmark;
import com.example.FinBuddy.services.BenchmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing portfolio benchmarks
 */
@RestController
@RequestMapping("/api/portfolios/{portfolioId}/benchmarks")
@CrossOrigin(origins = "*")
public class BenchmarkController {

    private static final Logger logger = LoggerFactory.getLogger(BenchmarkController.class);

    @Autowired
    private BenchmarkService benchmarkService;

    /**
     * Add a benchmark to a portfolio
     * POST /api/portfolios/{portfolioId}/benchmarks
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addBenchmark(
            @PathVariable Long portfolioId,
            @RequestBody BenchmarkRequest request) {

        logger.info("POST /api/portfolios/{}/benchmarks - Adding benchmark: {}", portfolioId, request.getSymbol());

        Map<String, Object> response = new HashMap<>();

        try {
            Benchmark benchmark = benchmarkService.addBenchmark(portfolioId, request);

            response.put("success", true);
            response.put("message", "Benchmark added successfully");
            response.put("data", benchmark);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error adding benchmark: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get all benchmarks for a portfolio
     * GET /api/portfolios/{portfolioId}/benchmarks
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getBenchmarks(@PathVariable Long portfolioId) {
        logger.info("GET /api/portfolios/{}/benchmarks - Fetching benchmarks", portfolioId);

        Map<String, Object> response = new HashMap<>();

        try {
            List<Benchmark> benchmarks = benchmarkService.getBenchmarks(portfolioId);

            response.put("success", true);
            response.put("data", benchmarks);
            response.put("count", benchmarks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching benchmarks: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete a benchmark
     * DELETE /api/portfolios/{portfolioId}/benchmarks/{benchmarkId}
     */
    @DeleteMapping("/{benchmarkId}")
    public ResponseEntity<Map<String, Object>> deleteBenchmark(
            @PathVariable Long portfolioId,
            @PathVariable Long benchmarkId) {

        logger.info("DELETE /api/portfolios/{}/benchmarks/{} - Deleting benchmark", portfolioId, benchmarkId);

        Map<String, Object> response = new HashMap<>();

        try {
            benchmarkService.deleteBenchmark(portfolioId, benchmarkId);

            response.put("success", true);
            response.put("message", "Benchmark deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error deleting benchmark: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Refresh all benchmark values
     * POST /api/portfolios/{portfolioId}/benchmarks/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshBenchmarks(@PathVariable Long portfolioId) {
        logger.info("POST /api/portfolios/{}/benchmarks/refresh - Refreshing benchmarks", portfolioId);

        Map<String, Object> response = new HashMap<>();

        try {
            List<Benchmark> benchmarks = benchmarkService.refreshBenchmarks(portfolioId);

            response.put("success", true);
            response.put("message", "Benchmarks refreshed successfully");
            response.put("data", benchmarks);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error refreshing benchmarks: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
