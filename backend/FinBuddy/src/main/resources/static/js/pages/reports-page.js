// ============================================
// Reports Page Logic
// ============================================

import { reportAPI } from "../utils/api.js";
import { showToast, showLoading, hideLoading } from "../utils/ui.js";

let currentPortfolioId = null;

async function initReportsPage() {
  setupEventListeners();
  window.addEventListener("portfolioChanged", handlePortfolioChange);
}

function setupEventListeners() {
  const downloadPdfBtn = document.getElementById("downloadPdfBtn");
  const downloadCsvBtn = document.getElementById("downloadCsvBtn");

  if (downloadPdfBtn) {
    downloadPdfBtn.addEventListener("click", handleDownloadPDF);
  }

  if (downloadCsvBtn) {
    downloadCsvBtn.addEventListener("click", handleDownloadCSV);
  }
}

function handlePortfolioChange(event) {
  currentPortfolioId = event.detail.portfolioId;
}

async function handleDownloadPDF() {
  if (!currentPortfolioId) {
    showToast("Please select a portfolio first", "warning");
    return;
  }

  showLoading();
  try {
    const blob = await reportAPI.downloadPDF(currentPortfolioId);
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `portfolio-report-${currentPortfolioId}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);

    showToast("PDF downloaded successfully", "success");
    hideLoading();
  } catch (error) {
    console.error("Error downloading PDF:", error);
    showToast("Failed to download PDF", "error");
    hideLoading();
  }
}

function handleDownloadCSV() {
  showToast("CSV export feature coming soon", "info");
}

// Initialize
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initReportsPage);
} else {
  initReportsPage();
}

export { initReportsPage };
