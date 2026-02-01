/**
 * FinBuddy Dashboard JavaScript
 * Handles API calls, chart rendering, and UI interactions
 */

// API Base URL
const API_BASE_URL = "http://localhost:8081/api";

// Global State
let currentPortfolioId = null;
let allocationChart = null;
let growthChart = null;
let performanceChart = null;

// Initialize Dashboard
document.addEventListener("DOMContentLoaded", () => {
  loadPortfolios();
  setupEventListeners();
  loadBenchmarks();
});

// ================== Event Listeners ==================
function setupEventListeners() {
  // Portfolio events
  document
    .getElementById("portfolioSelect")
    .addEventListener("change", handlePortfolioChange);
  document
    .getElementById("createPortfolioBtn")
    .addEventListener("click", openPortfolioModal);
  document
    .getElementById("portfolioForm")
    .addEventListener("submit", createPortfolio);

  // Asset events
  document
    .getElementById("toggleAddAssetBtn")
    .addEventListener("click", toggleAddAssetForm);
  document
    .getElementById("cancelAssetBtn")
    .addEventListener("click", toggleAddAssetForm);
  document.getElementById("assetForm").addEventListener("submit", createAsset);
  document.getElementById("searchBtn").addEventListener("click", searchAssets);

  // Refresh data
  document
    .getElementById("refreshDataBtn")
    .addEventListener("click", refreshData);

  // PDF download
  document
    .getElementById("downloadPdfBtn")
    .addEventListener("click", downloadPdf);

  // Enhanced features (if elements exist)
  const exportCsvBtn = document.getElementById("exportCsvBtn");
  if (exportCsvBtn) {
    exportCsvBtn.addEventListener("click", exportToCSV);
  }

  const shareBtn = document.getElementById("shareBtn");
  if (shareBtn) {
    shareBtn.addEventListener("click", sharePortfolio);
  }

  const themeToggle = document.getElementById("themeToggle");
  if (themeToggle) {
    themeToggle.addEventListener("click", toggleTheme);
  }

  // Modal close
  document.querySelectorAll(".close, .close-modal").forEach((el) => {
    el.addEventListener("click", closeModal);
  });
}

// ================== Portfolio Functions ==================
async function loadPortfolios() {
  try {
    const response = await fetch(`${API_BASE_URL}/portfolios`);
    const portfolios = await response.json();

    const select = document.getElementById("portfolioSelect");
    select.innerHTML = '<option value="">-- Select a Portfolio --</option>';

    portfolios.forEach((portfolio) => {
      const option = document.createElement("option");
      option.value = portfolio.id;
      option.textContent = portfolio.name;
      select.appendChild(option);
    });

    // Auto-select first portfolio if available
    if (portfolios.length > 0) {
      select.value = portfolios[0].id;
      handlePortfolioChange();
    }
  } catch (error) {
    console.error("Error loading portfolios:", error);
    showNotification("Failed to load portfolios", "error");
  }
}

async function handlePortfolioChange() {
  const select = document.getElementById("portfolioSelect");
  currentPortfolioId = select.value;

  if (!currentPortfolioId) {
    clearDashboard();
    return;
  }

  await loadDashboardData();

  // Initialize live prices if function exists (from enhancements.js)
  if (typeof initializeLivePrices === "function") {
    initializeLivePrices();
  }
}

async function createPortfolio(event) {
  event.preventDefault();

  const portfolio = {
    name: document.getElementById("portfolioName").value,
    description: document.getElementById("portfolioDescription").value,
    baseCurrency: document.getElementById("portfolioBaseCurrency").value,
  };

  try {
    const response = await fetch(`${API_BASE_URL}/portfolios`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(portfolio),
    });

    if (response.ok) {
      showNotification("Portfolio created successfully!", "success");
      closeModal();
      document.getElementById("portfolioForm").reset();
      await loadPortfolios();
    } else {
      showNotification("Failed to create portfolio", "error");
    }
  } catch (error) {
    console.error("Error creating portfolio:", error);
    showNotification("Failed to create portfolio", "error");
  }
}

// ================== Dashboard Data Functions ==================
async function loadDashboardData() {
  if (!currentPortfolioId) return;

  try {
    // Load dashboard summary
    const summaryResponse = await fetch(
      `${API_BASE_URL}/portfolios/${currentPortfolioId}/dashboard`,
    );
    const summary = await summaryResponse.json();

    // Update summary cards
    updateSummaryCards(summary);

    // Load and render charts
    renderAssetAllocationChart(summary.assetAllocation);
    await loadPortfolioHistory();
    renderAssetPerformanceChart(summary.topPerformers);

    // Load assets
    await loadAssets();
    await loadWishlistAssets();
  } catch (error) {
    console.error("Error loading dashboard data:", error);
    showNotification("Failed to load dashboard data", "error");
  }
}

function updateSummaryCards(summary) {
  document.getElementById("totalValue").textContent = formatCurrency(
    summary.totalValue,
    summary.baseCurrency,
  );
  document.getElementById("totalInvestment").textContent = formatCurrency(
    summary.totalInvestment,
    summary.baseCurrency,
  );

  const gainLoss = summary.totalGainLoss;
  const gainLossPercent = summary.gainLossPercentage;

  const gainLossEl = document.getElementById("gainLoss");
  const gainLossPercentEl = document.getElementById("gainLossPercent");

  gainLossEl.textContent = formatCurrency(gainLoss, summary.baseCurrency);
  gainLossPercentEl.textContent = `${gainLossPercent.toFixed(2)}%`;

  // Apply color coding
  const isPositive = gainLoss >= 0;
  gainLossEl.className = `card-value ${isPositive ? "positive" : "negative"}`;
  gainLossPercentEl.className = `card-percentage ${isPositive ? "positive" : "negative"}`;

  document.getElementById("assetCount").textContent = summary.assetCount;
}

// ================== Asset Functions ==================
async function loadAssets() {
  if (!currentPortfolioId) return;

  try {
    const response = await fetch(
      `${API_BASE_URL}/assets/portfolio/${currentPortfolioId}/invested`,
    );
    const assets = await response.json();

    const tbody = document.getElementById("assetsTableBody");
    tbody.innerHTML = "";

    if (assets.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="9" class="empty-state"><i class="fas fa-folder-open"></i><p>No assets found. Add your first asset!</p></td></tr>';
      return;
    }

    assets.forEach((asset) => {
      const row = createAssetRow(asset);
      tbody.appendChild(row);
    });
  } catch (error) {
    console.error("Error loading assets:", error);
  }
}

function createAssetRow(asset) {
  const row = document.createElement("tr");

  const isPositive = asset.gainLoss >= 0;
  const gainLossClass = isPositive ? "positive" : "negative";

  row.innerHTML = `
        <td>${asset.name}</td>
        <td><strong>${asset.symbol}</strong></td>
        <td><span class="badge">${asset.assetType}</span></td>
        <td>${asset.quantity}</td>
        <td class="live-price-cell" data-symbol="${asset.symbol}">
            <span class="price-loading">
                <i class="fas fa-sync fa-spin"></i> Loading...
            </span>
        </td>
        <td>${formatCurrency(asset.currentValue, asset.currency)}</td>
        <td class="${gainLossClass}">
            <i class="fas fa-arrow-${isPositive ? "up" : "down"}"></i>
            ${formatCurrency(asset.gainLoss, asset.currency)}
        </td>
        <td class="${gainLossClass}">${asset.gainLossPercentage.toFixed(2)}%</td>
        <td class="asset-actions">
            <button class="btn btn-danger btn-sm" onclick="deleteAsset(${asset.id})">
                <i class="fas fa-trash"></i> Delete
            </button>
        </td>
    `;

  // Fetch and update live price
  if (typeof livePrices !== "undefined" && livePrices[asset.symbol]) {
    updateLivePriceCell(row, asset.symbol, livePrices[asset.symbol]);
  } else {
    fetchPriceForRow(row, asset.symbol);
  }

  return row;
}

// Helper to fetch and update live price for a row
async function fetchPriceForRow(row, symbol) {
  try {
    const response = await fetch(`${API_BASE_URL}/market/price/${symbol}`);
    if (response.ok) {
      const data = await response.json();
      updateLivePriceCell(row, symbol, data.price);
    }
  } catch (error) {
    // Silently fail, keep loading state
  }
}

// Helper to update live price cell
function updateLivePriceCell(row, symbol, price) {
  const cell = row.querySelector(".live-price-cell");
  if (cell) {
    cell.innerHTML = `<strong style="color: var(--success-color);">$${parseFloat(price).toFixed(2)}</strong>`;
  }
}

async function loadWishlistAssets() {
  if (!currentPortfolioId) return;

  try {
    const response = await fetch(
      `${API_BASE_URL}/assets/portfolio/${currentPortfolioId}/wishlist`,
    );
    const assets = await response.json();

    const tbody = document.getElementById("wishlistTableBody");
    tbody.innerHTML = "";

    if (assets.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="5" class="empty-state">No wishlist items</td></tr>';
      return;
    }

    assets.forEach((asset) => {
      const row = document.createElement("tr");
      row.innerHTML = `
                <td>${asset.name}</td>
                <td>${asset.symbol}</td>
                <td>${asset.assetType}</td>
                <td>${formatCurrency(asset.currentValue, asset.currency)}</td>
                <td class="asset-actions">
                    <button class="btn btn-danger" onclick="deleteAsset(${asset.id})">Remove</button>
                </td>
            `;
      tbody.appendChild(row);
    });
  } catch (error) {
    console.error("Error loading wishlist:", error);
  }
}

async function createAsset(event) {
  event.preventDefault();

  if (!currentPortfolioId) {
    showNotification("Please select a portfolio first", "error");
    return;
  }

  const assetType = document.getElementById("assetType").value;
  const endpoint = getAssetEndpoint(assetType);

  const asset = {
    name: document.getElementById("assetName").value,
    symbol: document.getElementById("assetSymbol").value,
    quantity: parseInt(document.getElementById("assetQuantity").value),
    purchasePrice: parseFloat(document.getElementById("purchasePrice").value),
    currentPrice: parseFloat(document.getElementById("currentPrice").value),
    purchaseDate: document.getElementById("purchaseDate").value,
    currency: document.getElementById("currency").value,
    isWishlist: document.getElementById("isWishlist").checked,
  };

  try {
    const response = await fetch(
      `${API_BASE_URL}/assets/${endpoint}/${currentPortfolioId}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(asset),
      },
    );

    if (response.ok) {
      showNotification("Asset added successfully!", "success");
      document.getElementById("assetForm").reset();
      toggleAddAssetForm();
      await loadDashboardData();
    } else {
      showNotification("Failed to add asset", "error");
    }
  } catch (error) {
    console.error("Error creating asset:", error);
    showNotification("Failed to add asset", "error");
  }
}

function getAssetEndpoint(assetType) {
  const endpoints = {
    STOCK: "stocks",
    BOND: "bonds",
    MUTUAL_FUND: "mutualfunds",
    SIP: "sips",
  };
  return endpoints[assetType] || "stocks";
}

async function deleteAsset(assetId) {
  if (!confirm("Are you sure you want to delete this asset?")) return;

  try {
    const response = await fetch(`${API_BASE_URL}/assets/${assetId}`, {
      method: "DELETE",
    });

    if (response.ok) {
      showNotification("Asset deleted successfully!", "success");
      await loadDashboardData();
    } else {
      showNotification("Failed to delete asset", "error");
    }
  } catch (error) {
    console.error("Error deleting asset:", error);
    showNotification("Failed to delete asset", "error");
  }
}

async function searchAssets() {
  const query = document.getElementById("assetSearch").value;

  if (!query) {
    await loadAssets();
    return;
  }

  try {
    const response = await fetch(
      `${API_BASE_URL}/assets/search?query=${encodeURIComponent(query)}`,
    );
    const assets = await response.json();

    const tbody = document.getElementById("assetsTableBody");
    tbody.innerHTML = "";

    if (assets.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="8" class="empty-state">No assets found</td></tr>';
      return;
    }

    assets.forEach((asset) => {
      if (!asset.isWishlist) {
        const row = createAssetRow(asset);
        tbody.appendChild(row);
      }
    });
  } catch (error) {
    console.error("Error searching assets:", error);
  }
}

// ================== Chart Functions ==================
function renderAssetAllocationChart(allocation) {
  const ctx = document.getElementById("assetAllocationChart").getContext("2d");

  if (allocationChart) {
    allocationChart.destroy();
  }

  const labels = Object.keys(allocation);
  const data = labels.map((key) => allocation[key].totalValue);

  allocationChart = new Chart(ctx, {
    type: "pie",
    data: {
      labels: labels,
      datasets: [
        {
          data: data,
          backgroundColor: [
            "#2563eb",
            "#10b981",
            "#f59e0b",
            "#ef4444",
            "#8b5cf6",
            "#ec4899",
          ],
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: "bottom",
        },
        tooltip: {
          callbacks: {
            label: function (context) {
              const label = context.label || "";
              const value = context.parsed;
              const percentage = allocation[label].percentage.toFixed(2);
              return `${label}: $${value.toFixed(2)} (${percentage}%)`;
            },
          },
        },
      },
    },
  });
}

async function loadPortfolioHistory() {
  if (!currentPortfolioId) return;

  try {
    const response = await fetch(
      `${API_BASE_URL}/portfolios/${currentPortfolioId}/history`,
    );
    const history = await response.json();

    // If no history, create mock data for demo
    if (history.length === 0) {
      renderGrowthChart([]);
      return;
    }

    renderGrowthChart(history);
  } catch (error) {
    console.error("Error loading portfolio history:", error);
    renderGrowthChart([]);
  }
}

function renderGrowthChart(history) {
  const ctx = document.getElementById("portfolioGrowthChart").getContext("2d");

  if (growthChart) {
    growthChart.destroy();
  }

  // If no history data, show placeholder
  const labels =
    history.length > 0 ? history.map((h) => h.recordDate) : ["Start", "Now"];
  const values = history.length > 0 ? history.map((h) => h.totalValue) : [0, 0];

  growthChart = new Chart(ctx, {
    type: "line",
    data: {
      labels: labels,
      datasets: [
        {
          label: "Portfolio Value",
          data: values,
          borderColor: "#2563eb",
          backgroundColor: "rgba(37, 99, 235, 0.1)",
          tension: 0.4,
          fill: true,
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function (value) {
              return "$" + value.toLocaleString();
            },
          },
        },
      },
    },
  });
}

function renderAssetPerformanceChart(topPerformers) {
  const ctx = document.getElementById("assetPerformanceChart").getContext("2d");

  if (performanceChart) {
    performanceChart.destroy();
  }

  if (!topPerformers || topPerformers.length === 0) {
    return;
  }

  const labels = topPerformers.map((p) => p.name);
  const data = topPerformers.map((p) => p.gainLossPercentage);

  performanceChart = new Chart(ctx, {
    type: "bar",
    data: {
      labels: labels,
      datasets: [
        {
          label: "Gain/Loss %",
          data: data,
          backgroundColor: data.map((v) => (v >= 0 ? "#10b981" : "#ef4444")),
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      },
      scales: {
        y: {
          ticks: {
            callback: function (value) {
              return value + "%";
            },
          },
        },
      },
    },
  });
}

// ================== Benchmark Functions ==================
async function loadBenchmarks() {
  try {
    const sp500Response = await fetch(`${API_BASE_URL}/market/benchmark/^GSPC`);
    const sp500Data = await sp500Response.json();
    document.getElementById("sp500Value").textContent =
      sp500Data.value.toFixed(2);

    const niftyResponse = await fetch(`${API_BASE_URL}/market/benchmark/^NSEI`);
    const niftyData = await niftyResponse.json();
    document.getElementById("nifty50Value").textContent =
      niftyData.value.toFixed(2);
  } catch (error) {
    console.error("Error loading benchmarks:", error);
  }
}

// ================== PDF Download ==================
async function downloadPdf() {
  if (!currentPortfolioId) {
    showNotification("Please select a portfolio", "error");
    return;
  }

  try {
    const response = await fetch(
      `${API_BASE_URL}/reports/portfolio/${currentPortfolioId}/pdf`,
    );
    const blob = await response.blob();

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `portfolio_report_${currentPortfolioId}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);

    showNotification("PDF downloaded successfully!", "success");
  } catch (error) {
    console.error("Error downloading PDF:", error);
    showNotification("Failed to download PDF", "error");
  }
}

// ================== UI Helper Functions ==================
function toggleAddAssetForm() {
  const form = document.getElementById("addAssetForm");
  form.style.display = form.style.display === "none" ? "block" : "none";
}

function openPortfolioModal() {
  document.getElementById("portfolioModal").style.display = "block";
}

function closeModal() {
  document.getElementById("portfolioModal").style.display = "none";
}

function clearDashboard() {
  document.getElementById("totalValue").textContent = "$0.00";
  document.getElementById("totalInvestment").textContent = "$0.00";
  document.getElementById("gainLoss").textContent = "$0.00";
  document.getElementById("gainLossPercent").textContent = "0%";
  document.getElementById("assetCount").textContent = "0";
  document.getElementById("assetsTableBody").innerHTML =
    '<tr><td colspan="8" class="empty-state">Select a portfolio to view assets</td></tr>';
  document.getElementById("wishlistTableBody").innerHTML =
    '<tr><td colspan="5" class="empty-state">No wishlist items</td></tr>';
}

async function refreshData() {
  if (!currentPortfolioId) return;

  // Recalculate portfolio metrics
  await fetch(`${API_BASE_URL}/portfolios/${currentPortfolioId}/recalculate`, {
    method: "POST",
  });

  await loadDashboardData();
  showNotification("Data refreshed!", "success");
}

function showNotification(message, type) {
  // Simple notification (can be enhanced with a toast library)
  alert(message);
}

function formatCurrency(value, currency = "USD") {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: currency,
  }).format(value);
}

// Close modal on outside click
window.onclick = function (event) {
  const modal = document.getElementById("portfolioModal");
  if (event.target === modal) {
    closeModal();
  }
};
