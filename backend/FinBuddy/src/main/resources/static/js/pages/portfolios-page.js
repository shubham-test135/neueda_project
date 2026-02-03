// ============================================
// Portfolios Page Logic
// ============================================

import { portfolioAPI } from "../utils/api.js";
import {
  showToast,
  showLoading,
  hideLoading,
  formatCurrency,
  formatPercentage,
} from "../utils/ui.js";

async function initPortfoliosPage() {
  await loadPortfolios();
  setupEventListeners();
}

function setupEventListeners() {
  // Create portfolio button handlers
  const createBtns = document.querySelectorAll(
    "#createPortfolioBtn, #createFirstPortfolio",
  );
  createBtns.forEach((btn) => {
    if (btn) {
      btn.addEventListener("click", () => {
        const modal = document.getElementById("portfolioModal");
        if (modal) {
          modal.classList.add("active");
          modal.style.display = "flex";
        }
      });
    }
  });

  // Portfolio form submission
  const portfolioForm = document.getElementById("portfolioForm");
  if (portfolioForm) {
    portfolioForm.addEventListener("submit", handleCreatePortfolio);
  }
}

async function loadPortfolios() {
  showLoading();
  try {
    const portfolios = await portfolioAPI.getAll();
    renderPortfolios(portfolios);
    hideLoading();
  } catch (error) {
    console.error("Error loading portfolios:", error);
    showToast("Failed to load portfolios", "error");
    hideLoading();
  }
}

function renderPortfolios(portfolios) {
  const grid = document.getElementById("portfoliosGrid");
  if (!grid) return;

  if (portfolios.length === 0) {
    grid.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-briefcase"></i>
                <p>No portfolios found</p>
                <button class="btn btn-primary" id="createFirstPortfolio">
                    <i class="fas fa-plus"></i> Create Your First Portfolio
                </button>
            </div>
        `;
    return;
  }

  grid.innerHTML = portfolios
    .map(
      (portfolio) => `
        <div class="portfolio-card" data-id="${portfolio.id}">
            <div class="portfolio-card-header">
                <div class="portfolio-card-icon">
                    <i class="fas fa-briefcase"></i>
                </div>
                <div class="portfolio-card-actions">
                    <button class="portfolio-card-btn" onclick="editPortfolio(${portfolio.id})" title="Edit">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="portfolio-card-btn" onclick="deletePortfolio(${portfolio.id})" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="portfolio-card-body">
                <h3>${portfolio.name}</h3>
                <p>${portfolio.description || "No description"}</p>
                <div class="portfolio-card-stats">
                    <div class="portfolio-stat">
                        <span class="portfolio-stat-label">Total Value</span>
                        <span class="portfolio-stat-value">${formatCurrency(portfolio.totalValue || 0, portfolio.baseCurrency)}</span>
                    </div>
                    <div class="portfolio-stat">
                        <span class="portfolio-stat-label">Gain/Loss</span>
                        <span class="portfolio-stat-value ${(portfolio.gainLossPercentage || 0) >= 0 ? "positive" : "negative"}">
                            ${formatPercentage(portfolio.gainLossPercentage || 0)}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    `,
    )
    .join("");

  // Add click handlers to cards
  document.querySelectorAll(".portfolio-card").forEach((card) => {
    card.addEventListener("click", (e) => {
      if (!e.target.closest(".portfolio-card-btn")) {
        const id = card.dataset.id;
        window.location.href = `dashboard.html?portfolio=${id}`;
      }
    });
  });
}

async function handleCreatePortfolio(e) {
  e.preventDefault();

  const formData = {
    name: document.getElementById("portfolioName").value,
    description: document.getElementById("portfolioDescription").value,
    baseCurrency: document.getElementById("portfolioBaseCurrency").value,
  };

  showLoading();
  try {
    await portfolioAPI.create(formData);
    showToast("Portfolio created successfully", "success");

    // Close modal
    const modal = document.getElementById("portfolioModal");
    modal.classList.remove("active");
    modal.style.display = "none";

    // Reset form
    e.target.reset();

    // Reload portfolios
    await loadPortfolios();

    // Refresh navbar dropdown
    if (typeof window.refreshPortfolioDropdown === "function") {
      await window.refreshPortfolioDropdown();
    }

    hideLoading();
  } catch (error) {
    console.error("Error creating portfolio:", error);
    showToast("Failed to create portfolio", "error");
    hideLoading();
  }
}

// Global functions for card actions
window.editPortfolio = async function (id) {
  console.log("Edit portfolio:", id);
  // TODO: Implement edit functionality
};

window.deletePortfolio = async function (id) {
  if (confirm("Are you sure you want to delete this portfolio?")) {
    showLoading();
    try {
      await portfolioAPI.delete(id);
      showToast("Portfolio deleted successfully", "success");
      await loadPortfolios();
      hideLoading();
    } catch (error) {
      console.error("Error deleting portfolio:", error);
      showToast("Failed to delete portfolio", "error");
      hideLoading();
    }
  }
};

// Initialize
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initPortfoliosPage);
} else {
  initPortfoliosPage();
}

export { initPortfoliosPage };
