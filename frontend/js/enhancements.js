/**
 * Enhanced Features for Real-Time Stock Prices
 * Add this to the existing app.js
 */

// Auto-refresh interval (30 seconds)
let priceRefreshInterval = null;
const PRICE_REFRESH_INTERVAL = 30000; // 30 seconds

// Live prices state
let livePrices = {};
let tickerSymbols = [];

// ================== Real-Time Price Functions ==================

/**
 * Initialize live price updates
 */
function initializeLivePrices() {
  if (priceRefreshInterval) {
    clearInterval(priceRefreshInterval);
  }

  // Update prices immediately
  updateLivePrices();

  // Setup auto-refresh
  priceRefreshInterval = setInterval(() => {
    updateLivePrices();
  }, PRICE_REFRESH_INTERVAL);
}

/**
 * Update live prices for all assets
 */
async function updateLivePrices() {
  if (!currentPortfolioId) return;

  try {
    const assets = await fetchAssets(currentPortfolioId);

    if (assets.length === 0) {
      document.getElementById("livePricesGrid").innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-chart-area"></i>
                    <p>Add assets to see live prices</p>
                </div>
            `;
      return;
    }

    // Extract unique symbols
    tickerSymbols = [...new Set(assets.map((a) => a.symbol))];

    // Fetch live prices
    const prices = await fetchBatchPrices(tickerSymbols);
    livePrices = prices;

    // Update ticker and price cards
    updateStockTicker();
    renderLivePricesGrid(assets, prices);
  } catch (error) {
    console.error("Error updating live prices:", error);
  }
}

/**
 * Fetch batch prices from API
 */
async function fetchBatchPrices(symbols) {
  try {
    const response = await fetch(`${API_BASE_URL}/market/prices/batch`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(symbols),
    });

    if (!response.ok) throw new Error("Failed to fetch prices");

    return await response.json();
  } catch (error) {
    console.error("Error fetching batch prices:", error);
    return {};
  }
}

/**
 * Fetch detailed quote for a symbol
 */
async function fetchDetailedQuote(symbol) {
  try {
    const response = await fetch(`${API_BASE_URL}/market/quote/${symbol}`);
    if (!response.ok) throw new Error("Failed to fetch quote");
    return await response.json();
  } catch (error) {
    console.error(`Error fetching quote for ${symbol}:`, error);
    return null;
  }
}

/**
 * Update the stock ticker at the top
 */
function updateStockTicker() {
  const ticker = document.getElementById("stockTicker");

  if (tickerSymbols.length === 0) {
    ticker.innerHTML = '<span class="ticker-item">No assets to track</span>';
    return;
  }

  let tickerHTML = "";

  // Duplicate items for seamless scroll
  const items = [...tickerSymbols, ...tickerSymbols];

  items.forEach((symbol) => {
    const price = livePrices[symbol] || 0;
    const change = (Math.random() - 0.5) * 5; // Mock change for demo
    const changePercent = ((change / price) * 100).toFixed(2);
    const isPositive = change >= 0;

    tickerHTML += `
            <span class="ticker-item">
                <strong>${symbol}</strong>: 
                $${parseFloat(price).toFixed(2)} 
                <span class="${isPositive ? "positive" : "negative"}">
                    <i class="fas fa-arrow-${isPositive ? "up" : "down"}"></i>
                    ${Math.abs(changePercent)}%
                </span>
            </span>
        `;
  });

  ticker.innerHTML = tickerHTML;
}

/**
 * Render live prices grid
 */
function renderLivePricesGrid(assets, prices) {
  const grid = document.getElementById("livePricesGrid");

  if (assets.length === 0) {
    grid.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-chart-area"></i>
                <p>Add assets to see live prices</p>
            </div>
        `;
    return;
  }

  // Group assets by symbol and calculate totals
  const assetsBySymbol = {};
  assets.forEach((asset) => {
    if (!assetsBySymbol[asset.symbol]) {
      assetsBySymbol[asset.symbol] = {
        symbol: asset.symbol,
        name: asset.name,
        type: asset.assetType,
        totalQuantity: 0,
        totalValue: 0,
      };
    }
    assetsBySymbol[asset.symbol].totalQuantity += asset.quantity;
  });

  let html = "";

  Object.values(assetsBySymbol).forEach((asset) => {
    const currentPrice = prices[asset.symbol] || 0;
    const previousPrice = currentPrice * 0.98; // Mock previous price
    const change = currentPrice - previousPrice;
    const changePercent = ((change / previousPrice) * 100).toFixed(2);
    const isPositive = change >= 0;
    const totalValue = asset.totalQuantity * currentPrice;

    html += `
            <div class="price-card">
                <div class="price-card-header">
                    <span class="price-card-symbol">${asset.symbol}</span>
                    <span class="price-card-badge">${asset.type}</span>
                </div>
                <div class="price-card-value">$${parseFloat(currentPrice).toFixed(2)}</div>
                <div class="price-card-change ${isPositive ? "positive" : "negative"}">
                    <i class="fas fa-arrow-${isPositive ? "up" : "down"}"></i>
                    ${isPositive ? "+" : ""}${change.toFixed(2)} (${changePercent}%)
                </div>
                <div style="margin-top: 0.5rem; font-size: 0.75rem; color: var(--text-secondary);">
                    <i class="fas fa-coins"></i> ${asset.totalQuantity} shares = $${totalValue.toFixed(2)}
                </div>
            </div>
        `;
  });

  grid.innerHTML = html;
}

/**
 * Helper to fetch assets
 */
async function fetchAssets(portfolioId) {
  try {
    const response = await fetch(
      `${API_BASE_URL}/assets/portfolio/${portfolioId}`,
    );
    if (!response.ok) throw new Error("Failed to fetch assets");
    return await response.json();
  } catch (error) {
    console.error("Error fetching assets:", error);
    return [];
  }
}

/**
 * Show toast notification
 */
function showToast(message, type = "info") {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.className = `toast ${type} show`;

  setTimeout(() => {
    toast.classList.remove("show");
  }, 3000);
}

/**
 * Toggle theme (dark/light mode)
 */
function toggleTheme() {
  document.body.classList.toggle("dark-mode");
  const isDark = document.body.classList.contains("dark-mode");
  localStorage.setItem("theme", isDark ? "dark" : "light");

  const icon = document.querySelector("#themeToggle i");
  icon.className = isDark ? "fas fa-sun" : "fas fa-moon";
}

async function previewPdf() {
  if (!currentPortfolioId) {
    showToast("Please select a portfolio", "error");
    return;
  }
  const iframe = document.getElementById("pdfPreviewFrame");
  const container = document.getElementById("pdfPreviewContainer");
  // iframe.src = `/api/reports/portfolio/${currentPortfolioId}/pdf`;
  container.style.display = "block";
}

/**
 * Share portfolio (copy link)
 */
function sharePortfolio() {
  if (!currentPortfolioId) {
    showToast("Please select a portfolio first", "error");
    return;
  }

  const url = `${window.location.origin}?portfolio=${currentPortfolioId}`;

  if (navigator.clipboard) {
    navigator.clipboard
      .writeText(url)
      .then(() => {
        showToast("Portfolio link copied to clipboard!", "success");
      })
      .catch(() => {
        showToast("Failed to copy link", "error");
      });
  } else {
    // Fallback
    const input = document.createElement("input");
    input.value = url;
    document.body.appendChild(input);
    input.select();
    document.execCommand("copy");
    document.body.removeChild(input);
    showToast("Portfolio link copied!", "success");
  }
}

// ================== Enhanced Event Listeners ==================

// Add these to your existing setupEventListeners() function:
/*
document.getElementById('themeToggle')?.addEventListener('click', toggleTheme);
document.getElementById('exportCsvBtn')?.addEventListener('click', exportToCSV);
document.getElementById('shareBtn')?.addEventListener('click', sharePortfolio);
*/

// Initialize theme from localStorage
if (localStorage.getItem("theme") === "dark") {
  document.body.classList.add("dark-mode");
  const icon = document.querySelector("#themeToggle i");
  if (icon) icon.className = "fas fa-sun";
}

// Auto-start price updates when portfolio is loaded
const originalHandlePortfolioChange = window.handlePortfolioChange;
if (originalHandlePortfolioChange) {
  window.handlePortfolioChange = async function () {
    await originalHandlePortfolioChange();
    initializeLivePrices();
  };
}
