// ============================================
// Market Page Logic
// ============================================

import { assetAPI, marketAPI } from "../utils/api.js";
import { showToast, formatCurrency } from "../utils/ui.js";

let currentPortfolioId = null;
let priceUpdateInterval = null;

async function initMarketPage() {
  window.addEventListener("portfolioChanged", handlePortfolioChange);
  startAutoRefresh();
}

async function handlePortfolioChange(event) {
  const portfolioId = event.detail.portfolioId;
  if (portfolioId) {
    currentPortfolioId = portfolioId;
    await loadLivePrices(portfolioId);
  }
}

async function loadLivePrices(portfolioId) {
  try {
    const assets = await assetAPI.getByPortfolio(portfolioId);
    const symbols = assets.map((a) => a.symbol);

    if (symbols.length > 0) {
      const prices = await marketAPI.getBatchPrices(symbols);
      renderLivePrices(prices);
    } else {
      renderEmptyState();
    }
  } catch (error) {
    console.error("Error loading live prices:", error);
    showToast("Failed to load live prices", "error");
  }
}

function renderLivePrices(prices) {
  const grid = document.getElementById("livePricesGrid");
  if (!grid) return;

  grid.innerHTML = prices
    .map((price) => {
      const change = price.change || 0;
      const changePercent = price.changePercent || 0;

      return `
            <div class="price-card">
                <div class="price-card-header">
                    <div>
                        <div class="price-card-symbol">${price.symbol}</div>
                        <div class="price-card-name">${price.name || ""}</div>
                    </div>
                </div>
                <div class="price-card-price">${formatCurrency(price.price)}</div>
                <div class="price-card-change ${change >= 0 ? "positive" : "negative"}">
                    <i class="fas fa-arrow-${change >= 0 ? "up" : "down"}"></i>
                    ${change.toFixed(2)} (${changePercent.toFixed(2)}%)
                </div>
            </div>
        `;
    })
    .join("");
}

function renderEmptyState() {
  const grid = document.getElementById("livePricesGrid");
  if (grid) {
    grid.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-chart-area"></i>
                <p>Add assets to see live prices</p>
            </div>
        `;
  }
}

function startAutoRefresh() {
  // Refresh every 30 seconds
  priceUpdateInterval = setInterval(() => {
    if (currentPortfolioId) {
      loadLivePrices(currentPortfolioId);
    }
  }, 30000);
}

// Cleanup on page unload
window.addEventListener("beforeunload", () => {
  if (priceUpdateInterval) {
    clearInterval(priceUpdateInterval);
  }
});

// Initialize
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initMarketPage);
} else {
  initMarketPage();
}

export { initMarketPage };
