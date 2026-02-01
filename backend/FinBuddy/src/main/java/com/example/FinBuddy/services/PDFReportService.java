package com.example.FinBuddy.services;

import com.example.FinBuddy.dto.DashboardSummaryDTO;
import com.example.FinBuddy.entities.Asset;
import com.example.FinBuddy.entities.Portfolio;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating PDF reports
 * Uses iText 7 library for PDF generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PDFReportService {

    private final PortfolioService portfolioService;
    private final AssetService assetService;

    /**
     * Generate comprehensive portfolio report as PDF
     */
    public byte[] generatePortfolioReport(Long portfolioId) {
        try {
            Portfolio portfolio = portfolioService.getPortfolioWithAssets(portfolioId)
                    .orElseThrow(() -> new RuntimeException("Portfolio not found"));

            DashboardSummaryDTO dashboard = portfolioService.getDashboardSummary(portfolioId);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Report Header
            addReportHeader(document, portfolio);

            // Portfolio Summary
            addPortfolioSummary(document, dashboard);

            // Asset Allocation
            addAssetAllocation(document, dashboard);

            // Asset Details Table
            addAssetDetailsTable(document, portfolio.getAssets());

            // Top Performers
            addTopPerformers(document, dashboard);

            // Footer
            addReportFooter(document);

            document.close();

            log.info("PDF report generated successfully for portfolio: {}", portfolioId);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void addReportHeader(Document document, Portfolio portfolio) {
        Paragraph title = new Paragraph("Portfolio Performance Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        Paragraph portfolioName = new Paragraph("Portfolio: " + portfolio.getName())
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(portfolioName);

        Paragraph timestamp = new Paragraph("Generated on: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(timestamp);

        document.add(new Paragraph("\n"));
    }

    private void addPortfolioSummary(Document document, DashboardSummaryDTO dashboard) {
        document.add(new Paragraph("Portfolio Summary").setFontSize(16).setBold());

        Table table = new Table(2);
        table.addCell("Total Value");
        table.addCell(dashboard.getBaseCurrency() + " " + dashboard.getTotalValue());

        table.addCell("Total Investment");
        table.addCell(dashboard.getBaseCurrency() + " " + dashboard.getTotalInvestment());

        table.addCell("Gain/Loss");
        table.addCell(dashboard.getBaseCurrency() + " " + dashboard.getTotalGainLoss() +
                " (" + dashboard.getGainLossPercentage() + "%)");

        table.addCell("Number of Assets");
        table.addCell(String.valueOf(dashboard.getAssetCount()));

        table.addCell("Wishlist Items");
        table.addCell(String.valueOf(dashboard.getWishlistCount()));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addAssetAllocation(Document document, DashboardSummaryDTO dashboard) {
        document.add(new Paragraph("Asset Allocation").setFontSize(16).setBold());

        Table table = new Table(4);
        table.addHeaderCell("Asset Type");
        table.addHeaderCell("Total Value");
        table.addHeaderCell("Percentage");
        table.addHeaderCell("Count");

        dashboard.getAssetAllocation().forEach((type, allocation) -> {
            table.addCell(type);
            table.addCell(dashboard.getBaseCurrency() + " " + allocation.getTotalValue());
            table.addCell(allocation.getPercentage() + "%");
            table.addCell(String.valueOf(allocation.getCount()));
        });

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addAssetDetailsTable(Document document, List<Asset> assets) {
        document.add(new Paragraph("Asset Details").setFontSize(16).setBold());

        Table table = new Table(7);
        table.addHeaderCell("Name");
        table.addHeaderCell("Symbol");
        table.addHeaderCell("Type");
        table.addHeaderCell("Quantity");
        table.addHeaderCell("Current Value");
        table.addHeaderCell("Gain/Loss");
        table.addHeaderCell("G/L %");

        assets.stream()
                .filter(a -> !a.getIsWishlist())
                .forEach(asset -> {
                    table.addCell(asset.getName());
                    table.addCell(asset.getSymbol());
                    table.addCell(asset.getAssetType());
                    table.addCell(String.valueOf(asset.getQuantity()));
                    table.addCell(asset.getCurrency() + " " + asset.getCurrentValue());
                    table.addCell(asset.getCurrency() + " " + asset.getGainLoss());
                    table.addCell(asset.getGainLossPercentage() + "%");
                });

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTopPerformers(Document document, DashboardSummaryDTO dashboard) {
        document.add(new Paragraph("Top Performers").setFontSize(16).setBold());

        Table table = new Table(4);
        table.addHeaderCell("Name");
        table.addHeaderCell("Symbol");
        table.addHeaderCell("Current Value");
        table.addHeaderCell("Gain/Loss %");

        dashboard.getTopPerformers().forEach(performer -> {
            table.addCell(performer.getName());
            table.addCell(performer.getSymbol());
            table.addCell(String.valueOf(performer.getCurrentValue()));
            table.addCell(performer.getGainLossPercentage() + "%");
        });

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addReportFooter(Document document) {
        document.add(new Paragraph("\n\n"));
        Paragraph footer = new Paragraph("FinBuddy - Your Financial Portfolio Manager")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic();
        document.add(footer);
    }
}
