package com.example.FinBuddy.controllers;

import com.example.FinBuddy.services.EmailService;
import com.example.FinBuddy.services.PDFReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PDFReportService pdfReportService;
    private final EmailService emailService;

    public ReportController(PDFReportService pdfReportService, EmailService emailService) {
        this.pdfReportService = pdfReportService;
        this.emailService = emailService;
    }

    // Endpoint to generate and download the PDF report
    @GetMapping("/portfolio/{portfolioId}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long portfolioId) {
        try {
            // Generate PDF report
            byte[] pdfBytes = pdfReportService.generatePortfolioReport(portfolioId);

            // Return the PDF as a downloadable response
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=portfolio-report-" + portfolioId + ".pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Endpoint to send the PDF report via email
    @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> sendEmailReport(@RequestParam Long portfolioId, @RequestParam String email) {
        try {
            // Generate PDF report
            byte[] pdfBytes = pdfReportService.generatePortfolioReport(portfolioId);

            // Send email with attached PDF
            emailService.sendEmailWithAttachment(email, "Portfolio Report", "Here is your portfolio report.", pdfBytes);

            // Success response
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            // Error response
            return ResponseEntity.status(500).body(Map.of("success", false));
        }
    }
}
