import { reportAPI } from "../utils/api.js";
import { showToast, showLoading, hideLoading } from "../utils/ui.js";
import { initGlobalNavbar } from "../navbar.js";

let currentPortfolioId = null;

async function initReportsPage() {
  initGlobalNavbar();
  setupEventListeners();
  window.addEventListener("portfolioChanged", handlePortfolioChange);
}

function setupEventListeners() {
  const downloadPdfBtn = document.getElementById("downloadPdfBtn");
  const sendEmailBtn = document.getElementById("emailReportBtn"); // Focused on the email button

  if (downloadPdfBtn) {
    downloadPdfBtn.addEventListener("click", handleDownloadPDF);
  }

  if (sendEmailBtn) {  // Listen for clicks on the Email Report button
    sendEmailBtn.addEventListener("click", handleSendEmail);
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

// Handle Send Email Logic
async function handleSendEmail() {
  if (!currentPortfolioId) {
    showToast("Please select a portfolio first", "warning");
    return;
  }

  const email = prompt("Please enter the email address to send the report to:");

  if (!email || !validateEmail(email)) {
    showToast("Invalid email address", "error");
    return;
  }

  showLoading();

  try {
    // Call the API to send the report via email
    const response = await reportAPI.sendEmail(currentPortfolioId, email);

    if (response && response.success) {  // Ensure the response has the correct structure
      showToast("Email sent successfully!", "success");
    } else {
      showToast("Failed to send email", "error");
    }

    hideLoading();
  } catch (error) {
    console.error("Error sending email:", error);
    showToast("Error sending email", "error");
    hideLoading();
  }
}

function validateEmail(email) {
  // Simple email validation (you can make it more robust if needed)
  const re = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
  return re.test(email);
}

// Initialize the page
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initReportsPage);
} else {
  initReportsPage();
}

export { initReportsPage };
