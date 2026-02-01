package com.example.FinBuddy.controllers;

import com.example.FinBuddy.services.PDFReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for PDF Report generation
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final PDFReportService pdfReportService;

    /**
     * Generate and download portfolio PDF report
     */
    @GetMapping("/portfolio/{portfolioId}/pdf")
    public ResponseEntity<byte[]> downloadPortfolioReport(@PathVariable Long portfolioId) {
        try {
            byte[] pdfBytes = pdfReportService.generatePortfolioReport(portfolioId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "portfolio_report_" + portfolioId + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
