// ============================================
// Market Page Logic
// ============================================

import { assetAPI, marketAPI } from "../utils/api.js";
import { showToast, formatCurrency } from "../utils/ui.js";
import { initGlobalNavbar } from "../navbar.js";

let currentPortfolioId = null;
let priceUpdateInterval = null;

async function initMarketPage() {
  initGlobalNavbar();
  window.addEventListener("portfolioChanged", handlePortfolioChange);
  loadMarketIndices(); // Load indices on page load
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

async function loadMarketIndices() {
  try {
    // Load major market indices
    const indices = [
      { symbol: "^GSPC", id: "sp500" }, // S&P 500
      { symbol: "^IXIC", id: "nasdaq" }, // NASDAQ
      { symbol: "^NSEI", id: "nifty" }, // NIFTY 50
    ];

    for (const index of indices) {
      try {
        const data = await marketAPI.getBenchmark(index.symbol);
        updateIndexDisplay(index.id, data);
      } catch (error) {
        console.error(`Error loading ${index.symbol}:`, error);
        // Set error state for this index
        document.getElementById(`${index.id}Index`).textContent = "N/A";
        document.getElementById(`${index.id}Change`).textContent = "--";
      }
    }
  } catch (error) {
    console.error("Error loading market indices:", error);
  }
}

function updateIndexDisplay(indexId, data) {
  const valueElement = document.getElementById(`${indexId}Index`);
  const changeElement = document.getElementById(`${indexId}Change`);

  if (valueElement && data.value) {
    valueElement.textContent = formatCurrency(data.value);
  }

  if (changeElement && data.change !== undefined) {
    const change = data.change || 0;
    const changePercent = data.changePercent || 0;
    changeElement.textContent = `${change >= 0 ? "+" : ""}${changePercent.toFixed(2)}%`;
    changeElement.className = `benchmark-change ${change >= 0 ? "positive" : "negative"}`;
  }
}

function startAutoRefresh() {
  // Refresh every 30 seconds
  priceUpdateInterval = setInterval(() => {
    if (currentPortfolioId) {
      loadLivePrices(currentPortfolioId);
    }
    loadMarketIndices(); // Also refresh indices
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
