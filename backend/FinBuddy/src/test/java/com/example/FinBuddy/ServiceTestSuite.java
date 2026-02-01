package com.example.FinBuddy;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test Suite Runner for all FinBuddy Service Tests
 * 
 * This suite runs all service layer tests together.
 * Run this class to execute all service tests at once.
 * 
 * Coverage:
 * - PortfolioServiceTest: Portfolio CRUD and metrics calculation
 * - AssetServiceTest: Asset management and operations
 * - StockPriceServiceTest: Stock price fetching and caching
 * - PDFReportServiceTest: PDF report generation
 * 
 * To run from command line:
 * mvn test -Dtest=ServiceTestSuite
 */
@Suite
@SuiteDisplayName("FinBuddy Service Layer Test Suite")
@SelectPackages("com.example.FinBuddy.services")
public class ServiceTestSuite {
    // This class remains empty - it's used only as a holder for the annotations
}
