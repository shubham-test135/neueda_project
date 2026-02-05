// ============================================
// Wishlist Page - Complete Implementation
// ============================================

import { marketAPI, wishlistAPI, assetAPI } from "../utils/api.js";
import { showToast, formatCurrency, updateExchangeRate } from "../utils/ui.js";
import { initGlobalNavbar, getCurrentCurrency } from "../navbar.js";

let wishlistItems = [];
let currentPortfolioId = 1;
let performanceChart = null;
let currentAssetData = null;
let autoRefreshInterval = null;
const AUTO_REFRESH_INTERVAL = 60000; // 60 seconds

// ===== Initialization =====
async function initWishlistPage() {
  initGlobalNavbar();

  const storedPortfolioId = sessionStorage.getItem("current_portfolio_id");
  if (storedPortfolioId) {
    currentPortfolioId = parseInt(storedPortfolioId);
  }

  // Initialize exchange rate for currency conversion
  await updateExchangeRate();

  await loadWishlist();
  setupEventListeners();

  // Start auto-refresh for prices
  startAutoRefresh();

  // Listen for currency changes
  window.addEventListener("currencyChanged", handleCurrencyChange);

  // Listen for portfolio changes
  window.addEventListener("portfolioChanged", handlePortfolioChange);
}

// Handle currency change event
async function handleCurrencyChange() {
  await updateExchangeRate();
  renderWishlist();
  await updateSummary();
}

// Handle portfolio change event
async function handlePortfolioChange(event) {
  if (event.detail?.portfolioId) {
    currentPortfolioId = parseInt(event.detail.portfolioId);
    sessionStorage.setItem("current_portfolio_id", currentPortfolioId);
    await loadWishlist();
  }
}

// Start auto-refresh interval
function startAutoRefresh() {
  if (autoRefreshInterval) clearInterval(autoRefreshInterval);

  autoRefreshInterval = setInterval(async () => {
    await silentRefreshPrices();
  }, AUTO_REFRESH_INTERVAL);
}

// Silent refresh without loading overlay
async function silentRefreshPrices() {
  try {
    const response = await wishlistAPI.refreshPrices(currentPortfolioId);
    if (response?.success) {
      wishlistItems = response.data || [];
      renderWishlist();
      await updateSummary();
      checkAlerts(); // Check for triggered alerts
    }
  } catch (error) {
    console.error("Silent refresh failed:", error);
  }
}

// Check for triggered alerts and notify user
function checkAlerts() {
  const triggeredAlerts = wishlistItems.filter(item =>
    item.alertTriggered && item.alertEnabled
  );

  triggeredAlerts.forEach(item => {
    showToast(
      `🔔 Alert: ${item.symbol} has reached your target price of ${formatCurrency(item.targetPrice, item.currency)}!`,
      "success"
    );
  });
}

// ===== Event Listeners =====
function setupEventListeners() {
  // Add to wishlist buttons
  document.getElementById("addToWishlistBtn")?.addEventListener("click", toggleAddForm);
  document.getElementById("addFirstItem")?.addEventListener("click", toggleAddForm);

  // Cancel buttons
  document.getElementById("cancelWishlistBtn")?.addEventListener("click", hideAddForm);
  document.getElementById("cancelWishlistBtn2")?.addEventListener("click", hideAddForm);

  // Form submission
  document.getElementById("wishlistForm")?.addEventListener("submit", handleAddToWishlist);

  // Search input with debounce
  const nameInput = document.getElementById("wishlistAssetName");
  if (nameInput) {
    const debouncedSearch = debounce(searchAssets, 300);
    nameInput.addEventListener("input", (e) => debouncedSearch(e.target.value));

    document.addEventListener("click", (e) => {
      const results = document.getElementById("wishlistSearchResults");
      if (results && !nameInput.contains(e.target) && !results.contains(e.target)) {
        results.style.display = "none";
      }
    });
  }

  // Asset type change triggers new search
  document.getElementById("wishlistAssetType")?.addEventListener("change", () => {
    const query = document.getElementById("wishlistAssetName")?.value;
    if (query?.length >= 2) searchAssets(query);
    document.getElementById("wishlistAssetSymbol").value = "";
  });

  // Filter and search
  document.getElementById("wishlistSearch")?.addEventListener("input", filterWishlist);
  document.getElementById("categoryFilter")?.addEventListener("change", filterWishlist);
  document.getElementById("sortBy")?.addEventListener("change", sortWishlist);

  // Refresh prices
  document.getElementById("refreshPricesBtn")?.addEventListener("click", refreshPrices);

  // Modal event listeners
  document.getElementById("closeDetailsModal")?.addEventListener("click", closeDetailsModal);
  document.getElementById("closePortfolioModal")?.addEventListener("click", closePortfolioModal);
  document.getElementById("cancelPortfolioAdd")?.addEventListener("click", closePortfolioModal);
  document.getElementById("modalRemoveBtn")?.addEventListener("click", handleModalRemove);
  document.getElementById("modalAddToPortfolioBtn")?.addEventListener("click", openAddToPortfolioModal);
  document.getElementById("addToPortfolioForm")?.addEventListener("submit", handleAddToPortfolio);

  // Time period selector
  document.querySelectorAll(".period-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {
      document.querySelectorAll(".period-btn").forEach(b => b.classList.remove("active"));
      e.target.classList.add("active");
      if (currentAssetData) {
        updateChart(currentAssetData.symbol, e.target.dataset.period);
      }
    });
  });

  // Close modals on overlay click
  document.getElementById("assetDetailsModal")?.addEventListener("click", (e) => {
    if (e.target.id === "assetDetailsModal") closeDetailsModal();
  });
  document.getElementById("addToPortfolioModal")?.addEventListener("click", (e) => {
    if (e.target.id === "addToPortfolioModal") closePortfolioModal();
  });
}

// ===== Load Wishlist =====
async function loadWishlist() {
  showLoading(true);
  try {
    const response = await wishlistAPI.getAll(currentPortfolioId);
    wishlistItems = response?.success ? (response.data || []) : [];
    await updateSummary();
    renderWishlist();
  } catch (error) {
    console.error("Error loading wishlist:", error);
    showToast("Failed to load wishlist", "error");
    wishlistItems = [];
    renderWishlist();
  } finally {
    showLoading(false);
  }
}

// ===== Update Summary =====
async function updateSummary() {
  try {
    const response = await wishlistAPI.getSummary(currentPortfolioId);
    if (response?.success) {
      const s = response.data;

      // Update card values
      document.getElementById("totalWatchlist").textContent = s.totalWatchlist || 0;
      document.getElementById("gainersCount").textContent = s.gainersCount || 0;
      document.getElementById("losersCount").textContent = s.losersCount || 0;
      document.getElementById("alertsCount").textContent = s.alertsCount || 0;

      // Show alert icon animation if there are triggered alerts
      const triggeredCount = wishlistItems.filter(i => i.alertTriggered).length;
      const alertsCard = document.querySelector('.card-icon.gradient-orange');
      if (alertsCard) {
        if (triggeredCount > 0) {
          alertsCard.classList.add('alert-pulse');
          alertsCard.title = `${triggeredCount} alert(s) triggered!`;
        } else {
          alertsCard.classList.remove('alert-pulse');
          alertsCard.title = 'Active price alerts';
        }
      }
    }
  } catch (error) {
    // Fallback to local calculation
    document.getElementById("totalWatchlist").textContent = wishlistItems.length;
    document.getElementById("gainersCount").textContent = wishlistItems.filter(i => (i.changePercentage || 0) > 0).length;
    document.getElementById("losersCount").textContent = wishlistItems.filter(i => (i.changePercentage || 0) < 0).length;
    document.getElementById("alertsCount").textContent = wishlistItems.filter(i => i.alertEnabled).length;
  }

  // Update last refresh time display
  updateLastRefreshTime();
}

// Update last refresh time in header
function updateLastRefreshTime() {
  const refreshBtn = document.getElementById("refreshPricesBtn");
  if (refreshBtn) {
    const now = new Date();
    refreshBtn.title = `Last updated: ${now.toLocaleTimeString()}. Click to refresh manually.`;
  }
}

// ===== Render Wishlist Table =====
function renderWishlist() {
  const tbody = document.getElementById("wishlistTableBody");
  const table = document.getElementById("wishlistTable");
  const empty = document.getElementById("emptyWishlist");

  if (!wishlistItems || wishlistItems.length === 0) {
    table.style.display = "none";
    empty.style.display = "flex";
    return;
  }

  table.style.display = "table";
  empty.style.display = "none";

  tbody.innerHTML = wishlistItems.map(item => {
    const change = item.changePercentage || 0;
    const performance = item.performanceSinceAdded || 0;
    const changeClass = change >= 0 ? "positive" : "negative";
    const perfClass = performance >= 0 ? "positive" : "negative";
    const typeClass = (item.category || "stock").toLowerCase().replace(" ", "_");

    // Alert status
    const alertTriggered = item.alertTriggered && item.alertEnabled;
    const alertActive = item.alertEnabled && !item.alertTriggered;
    const alertClass = alertTriggered ? "alert-triggered" : (alertActive ? "alert-active" : "");
    const alertIcon = alertTriggered ? "fa-bell-slash" : (alertActive ? "fa-bell" : "fa-bell");
    const alertColor = alertTriggered ? "#ef4444" : (alertActive ? "#f59e0b" : "#94a3b8");

    // Format target price with conversion
    const targetDisplay = item.targetPrice
      ? formatCurrency(item.targetPrice, item.currency)
      : "-";

    return `
      <tr data-id="${item.id}" class="${alertTriggered ? 'row-alert-triggered' : ''}">
        <td>
          <div class="asset-cell clickable" onclick="viewDetails(${item.id})" title="Click to view chart">
            <div class="asset-icon">${(item.symbol || "?").substring(0, 2)}</div>
            <div class="asset-cell-info">
              <h4>${item.symbol || "N/A"} <i class="fas fa-chart-line chart-hint"></i></h4>
              <span>${item.name || item.symbol}</span>
            </div>
          </div>
        </td>
        <td><span class="type-badge ${typeClass}">${formatCategory(item.category)}</span></td>
        <td class="price-cell">${formatCurrency(item.currentPrice || 0, item.currency)}</td>
        <td>
          <span class="change-cell ${changeClass}">
            <i class="fas fa-arrow-${change >= 0 ? "up" : "down"}"></i>
            ${change >= 0 ? "+" : ""}${change.toFixed(2)}%
          </span>
        </td>
        <td class="price-cell">${formatCurrency(item.priceWhenAdded || 0, item.currency)}</td>
        <td>
          <span class="change-cell ${perfClass}">
            ${performance >= 0 ? "+" : ""}${performance.toFixed(2)}%
          </span>
        </td>
        <td>
          <div class="target-cell ${alertClass}">
            <i class="fas ${alertIcon}" style="color: ${alertColor}; margin-right: 6px;" 
               title="${alertTriggered ? 'Alert Triggered!' : (alertActive ? 'Alert Active' : 'No Alert')}"></i>
            <input type="number" step="0.01" value="${item.targetPrice || ""}" 
              placeholder="Set target" 
              onchange="updateTargetPrice(${item.id}, this.value)"
              class="${alertTriggered ? 'input-triggered' : ''}"/>
            ${alertTriggered ? '<span class="alert-badge">Triggered!</span>' : ''}
          </div>
        </td>
        <td>
          <div class="action-buttons">
            <button class="action-btn" onclick="viewDetails(${item.id})" title="View Details">
              <i class="fas fa-chart-line"></i>
            </button>
            <button class="action-btn primary" onclick="openPortfolioModal(${item.id})" title="Add to Portfolio">
              <i class="fas fa-plus"></i>
            </button>
            <button class="action-btn danger" onclick="removeFromWishlist(${item.id})" title="Remove">
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join("");
}

function formatCategory(category) {
  if (!category) return "Stock";
  const map = {
    "STOCK": "Stock",
    "BOND": "Bond",
    "MUTUAL_FUND": "Mutual Fund",
    "SIP": "SIP"
  };
  return map[category.toUpperCase()] || category;
}

// ===== Form Handling =====
function toggleAddForm() {
  const form = document.getElementById("addWishlistForm");
  form.style.display = form.style.display === "none" ? "block" : "none";
  if (form.style.display === "block") {
    form.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }
}

function hideAddForm() {
  document.getElementById("addWishlistForm").style.display = "none";
  document.getElementById("wishlistForm").reset();
}

async function handleAddToWishlist(e) {
  e.preventDefault();
  showLoading(true);

  try {
    const requestData = {
      symbol: document.getElementById("wishlistAssetSymbol").value.toUpperCase(),
      category: document.getElementById("wishlistAssetType").value,
      targetPrice: document.getElementById("wishlistTargetPrice").value || null
    };

    const response = await wishlistAPI.add(currentPortfolioId, requestData);

    if (response?.success) {
      await loadWishlist();
      showToast(response.message || "Added to wishlist", "success");
      hideAddForm();
    } else {
      throw new Error(response?.message || "Failed to add");
    }
  } catch (error) {
    console.error("Error adding to wishlist:", error);
    showToast(error.message || "Failed to add to wishlist", "error");
  } finally {
    showLoading(false);
  }
}

// ===== Search Assets =====
async function searchAssets(query) {
  const results = document.getElementById("wishlistSearchResults");

  if (!query || query.trim().length < 2) {
    results.style.display = "none";
    return;
  }

  try {
    const assetType = document.getElementById("wishlistAssetType").value;
    let data;

    switch (assetType) {
      case "STOCK": data = await marketAPI.searchStocks(query); break;
      case "BOND": data = await marketAPI.searchBonds(query); break;
      case "MUTUAL_FUND": data = await marketAPI.searchMutualFunds(query); break;
      case "SIP": data = await marketAPI.searchSIPs(query); break;
      default: data = await marketAPI.searchAllAssets(query);
    }

    displaySearchResults(data);
  } catch (error) {
    console.error("Search error:", error);
    results.style.display = "none";
  }
}

function displaySearchResults(results) {
  const container = document.getElementById("wishlistSearchResults");

  if (!results || results.length === 0) {
    container.innerHTML = '<div class="search-result-item no-results">No results found</div>';
    container.style.display = "block";
    return;
  }

  container.innerHTML = results.slice(0, 10).map(r => {
    const type = r.type || "Stock";
    const typeClass = type.toLowerCase().replace(" ", "-");
    const name = (r.name || "").replace(/'/g, "\\'");

    let extra = "";
    if (type.toUpperCase() === "BOND") {
      extra = `<span class="result-extra">${r.creditRating || ""} | ${r.couponRate}%</span>`;
    } else if (type.toUpperCase().includes("MUTUAL") || type.toUpperCase() === "SIP") {
      extra = `<span class="result-extra">${r.fundHouse || ""}</span>`;
    } else {
      extra = `<span class="result-extra">${r.sector || r.exchange || ""}</span>`;
    }

    return `
      <div class="search-result-item" onclick="selectAsset('${r.symbol}', '${name}', '${type}')">
        <div class="result-main">
          <span class="result-symbol">${r.symbol}</span>
          <span class="result-name">${r.name}</span>
        </div>
        <div class="result-details">
          <span class="result-type ${typeClass}">${type}</span>
          ${extra}
          <span class="result-price">${formatCurrency(r.price || 0, r.currency || "USD")}</span>
        </div>
      </div>
    `;
  }).join("");

  container.style.display = "block";
}

function selectAsset(symbol, name, type) {
  document.getElementById("wishlistAssetSymbol").value = symbol;
  document.getElementById("wishlistAssetName").value = name;

  const typeMap = { "stock": "STOCK", "bond": "BOND", "mutual fund": "MUTUAL_FUND", "sip": "SIP" };
  document.getElementById("wishlistAssetType").value = typeMap[type.toLowerCase()] || "STOCK";
  document.getElementById("wishlistSearchResults").style.display = "none";
}

// ===== Filter & Sort =====
function filterWishlist() {
  const search = document.getElementById("wishlistSearch").value.toLowerCase().trim();
  const category = document.getElementById("categoryFilter").value;

  let filtered = [...wishlistItems];

  if (search) {
    filtered = filtered.filter(i =>
      (i.symbol || "").toLowerCase().includes(search) ||
      (i.name || "").toLowerCase().includes(search)
    );
  }

  if (category && category !== "all") {
    filtered = filtered.filter(i => (i.category || "").toLowerCase() === category.toLowerCase());
  }

  const original = wishlistItems;
  wishlistItems = filtered;
  renderWishlist();
  wishlistItems = original;
}

function sortWishlist() {
  const sortBy = document.getElementById("sortBy").value;

  switch (sortBy) {
    case "name":
      wishlistItems.sort((a, b) => (a.symbol || "").localeCompare(b.symbol || ""));
      break;
    case "price":
      wishlistItems.sort((a, b) => (b.currentPrice || 0) - (a.currentPrice || 0));
      break;
    case "change":
      wishlistItems.sort((a, b) => (b.changePercentage || 0) - (a.changePercentage || 0));
      break;
    case "added":
      wishlistItems.sort((a, b) => new Date(b.addedAt || 0) - new Date(a.addedAt || 0));
      break;
  }
  renderWishlist();
}

// ===== Actions =====
async function removeFromWishlist(itemId) {
  if (!confirm("Remove this item from your wishlist?")) return;

  showLoading(true);
  try {
    const response = await wishlistAPI.delete(currentPortfolioId, itemId);
    if (response?.success) {
      await loadWishlist();
      showToast("Removed from wishlist", "success");
    } else {
      throw new Error(response?.message);
    }
  } catch (error) {
    showToast("Failed to remove", "error");
  } finally {
    showLoading(false);
  }
}

async function updateTargetPrice(itemId, price) {
  try {
    const targetPrice = price ? parseFloat(price) : null;

    const response = await wishlistAPI.update(currentPortfolioId, itemId, {
      targetPrice: targetPrice
    });

    if (response?.success) {
      const item = wishlistItems.find(i => i.id === itemId);
      if (item) {
        item.targetPrice = targetPrice;
        item.alertEnabled = targetPrice !== null;
        item.alertTriggered = false; // Reset triggered state when target changes
      }

      await updateSummary();
      renderWishlist(); // Re-render to update alert icons

      if (targetPrice) {
        showToast(`Alert set: You'll be notified when price reaches ${formatCurrency(targetPrice, item?.currency)}`, "success");
      } else {
        showToast("Price alert removed", "info");
      }
    }
  } catch (error) {
    showToast("Failed to update target price", "error");
  }
}

async function refreshPrices() {
  showLoading(true);
  try {
    const response = await wishlistAPI.refreshPrices(currentPortfolioId);
    if (response?.success) {
      wishlistItems = response.data || [];
      renderWishlist();
      await updateSummary();
      showToast("Prices refreshed", "success");
    }
  } catch (error) {
    showToast("Failed to refresh prices", "error");
  } finally {
    showLoading(false);
  }
}

// ===== View Details Modal =====
function viewDetails(itemId) {
  const item = wishlistItems.find(i => i.id === itemId);
  if (!item) return;

  currentAssetData = item;

  // Populate modal
  document.getElementById("modalAssetSymbol").textContent = item.symbol;
  document.getElementById("modalAssetName").textContent = item.name || item.symbol;
  document.getElementById("modalCurrentPrice").textContent = formatCurrency(item.currentPrice || 0, item.currency);

  const change = item.changePercentage || 0;
  const changeEl = document.getElementById("modalChange");
  changeEl.textContent = `${change >= 0 ? "+" : ""}${change.toFixed(2)}%`;
  changeEl.className = `stat-value ${change >= 0 ? "positive" : "negative"}`;

  document.getElementById("modalAddedPrice").textContent = formatCurrency(item.priceWhenAdded || 0, item.currency);

  const perf = item.performanceSinceAdded || 0;
  const perfEl = document.getElementById("modalPerformance");
  perfEl.textContent = `${perf >= 0 ? "+" : ""}${perf.toFixed(2)}%`;
  perfEl.className = `stat-value ${perf >= 0 ? "positive" : "negative"}`;

  document.getElementById("modalAddedDate").textContent = item.addedAt ? new Date(item.addedAt).toLocaleDateString() : "-";
  document.getElementById("modalTargetPrice").textContent = item.targetPrice ? formatCurrency(item.targetPrice, item.currency) : "Not set";
  document.getElementById("modalCurrency").textContent = item.currency || "USD";
  document.getElementById("modalCategory").textContent = formatCategory(item.category);

  // Show modal
  document.getElementById("assetDetailsModal").style.display = "flex";

  // Update chart
  updateChart(item.symbol, "3M");
}

function closeDetailsModal() {
  document.getElementById("assetDetailsModal").style.display = "none";
  currentAssetData = null;
}

function handleModalRemove() {
  if (currentAssetData) {
    closeDetailsModal();
    removeFromWishlist(currentAssetData.id);
  }
}

// ===== Performance Chart =====
const FINNHUB_API_KEY = "d5vg7hpr01qjj9jjd8k0d5vg7hpr01qjj9jjd8kg";

async function updateChart(symbol, period) {
  const chartContainer = document.querySelector('.chart-container');
  const ctx = document.getElementById("assetPerformanceChart").getContext("2d");

  // Show loading state
  if (chartContainer) {
    chartContainer.classList.add('chart-loading');
  }

  try {
    // Fetch historical data
    const data = await fetchHistoricalData(symbol, period);

    if (performanceChart) {
      performanceChart.destroy();
    }

    const isPositive = data.values[data.values.length - 1] >= data.values[0];
    const gradientColor = isPositive ? "rgba(16, 185, 129, " : "rgba(239, 68, 68, ";

    const gradient = ctx.createLinearGradient(0, 0, 0, 280);
    gradient.addColorStop(0, gradientColor + "0.3)");
    gradient.addColorStop(1, gradientColor + "0)");

    // Calculate stats
    const startPrice = data.values[0];
    const endPrice = data.values[data.values.length - 1];
    const priceChange = endPrice - startPrice;
    const percentChange = ((priceChange / startPrice) * 100).toFixed(2);
    const highPrice = Math.max(...data.values);
    const lowPrice = Math.min(...data.values);

    // Update chart stats display
    updateChartStats(startPrice, endPrice, highPrice, lowPrice, percentChange, period);

    performanceChart = new Chart(ctx, {
      type: "line",
      data: {
        labels: data.labels,
        datasets: [{
          label: symbol,
          data: data.values,
          fill: true,
          backgroundColor: gradient,
          borderColor: isPositive ? "#10b981" : "#ef4444",
          borderWidth: 2,
          tension: 0.3,
          pointRadius: 0,
          pointHoverRadius: 6,
          pointHoverBackgroundColor: isPositive ? "#10b981" : "#ef4444",
          pointHoverBorderColor: "#fff",
          pointHoverBorderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          intersect: false,
          mode: "index"
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: "rgba(30, 41, 59, 0.95)",
            padding: 12,
            cornerRadius: 8,
            titleFont: { size: 13, weight: "600" },
            bodyFont: { size: 12 },
            displayColors: false,
            callbacks: {
              title: (ctx) => ctx[0].label,
              label: (ctx) => {
                const price = ctx.parsed.y.toFixed(2);
                const changeFromStart = ((ctx.parsed.y - startPrice) / startPrice * 100).toFixed(2);
                return [
                  `Price: $${price}`,
                  `Change: ${changeFromStart >= 0 ? '+' : ''}${changeFromStart}%`
                ];
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: {
              color: "#94a3b8",
              maxTicksLimit: 8,
              font: { size: 11 }
            }
          },
          y: {
            grid: { color: "rgba(148, 163, 184, 0.08)" },
            ticks: {
              color: "#94a3b8",
              font: { size: 11 },
              callback: (value) => "$" + value.toFixed(0)
            }
          }
        }
      }
    });
  } catch (error) {
    console.error("Error updating chart:", error);
    showToast("Failed to load chart data", "error");
  } finally {
    if (chartContainer) {
      chartContainer.classList.remove('chart-loading');
    }
  }
}

// Update chart statistics display
function updateChartStats(startPrice, endPrice, highPrice, lowPrice, percentChange, period) {
  const statsContainer = document.getElementById('chartStatsContainer');
  if (!statsContainer) return;

  const periodLabels = {
    "1W": "1 Week",
    "1M": "1 Month",
    "3M": "3 Months",
    "6M": "6 Months",
    "1Y": "1 Year",
    "ALL": "All Time"
  };

  statsContainer.innerHTML = `
    <div class="chart-stat">
      <span class="chart-stat-label">Period</span>
      <span class="chart-stat-value">${periodLabels[period] || period}</span>
    </div>
    <div class="chart-stat">
      <span class="chart-stat-label">Start</span>
      <span class="chart-stat-value">$${startPrice.toFixed(2)}</span>
    </div>
    <div class="chart-stat">
      <span class="chart-stat-label">High</span>
      <span class="chart-stat-value positive">$${highPrice.toFixed(2)}</span>
    </div>
    <div class="chart-stat">
      <span class="chart-stat-label">Low</span>
      <span class="chart-stat-value negative">$${lowPrice.toFixed(2)}</span>
    </div>
    <div class="chart-stat">
      <span class="chart-stat-label">Change</span>
      <span class="chart-stat-value ${parseFloat(percentChange) >= 0 ? 'positive' : 'negative'}">
        ${percentChange >= 0 ? '+' : ''}${percentChange}%
      </span>
    </div>
  `;
}

// Fetch historical data from Finnhub API
async function fetchHistoricalData(symbol, period) {
  const periods = { "1W": 7, "1M": 30, "3M": 90, "6M": 180, "1Y": 365, "ALL": 730 };
  const days = periods[period] || 90;

  const currentItem = wishlistItems.find(i => i.symbol === symbol);
  const currentPrice = currentItem?.currentPrice || 100;

  // Calculate timestamps
  const now = Math.floor(Date.now() / 1000);
  const from = now - (days * 24 * 60 * 60);

  try {
    // Try to fetch real data from Finnhub
    const resolution = days <= 7 ? "15" : days <= 30 ? "60" : "D"; // 15min, hourly, or daily
    const url = `https://finnhub.io/api/v1/stock/candle?symbol=${symbol}&resolution=${resolution}&from=${from}&to=${now}&token=${FINNHUB_API_KEY}`;

    const response = await fetch(url);
    const data = await response.json();

    if (data.s === "ok" && data.c && data.c.length > 0) {
      // Real data available
      const labels = data.t.map(timestamp => {
        const date = new Date(timestamp * 1000);
        if (period === "1W") {
          return date.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" });
        } else if (period === "1M") {
          return date.toLocaleDateString("en-US", { month: "short", day: "numeric" });
        } else {
          return date.toLocaleDateString("en-US", { month: "short", year: "2-digit" });
        }
      });

      // Sample data if too many points
      let values = data.c;
      if (values.length > 100) {
        const step = Math.ceil(values.length / 100);
        values = values.filter((_, i) => i % step === 0);
        const sampledLabels = labels.filter((_, i) => i % step === 0);
        return { labels: sampledLabels, values };
      }

      return { labels, values };
    }
  } catch (error) {
    console.warn("Finnhub API failed, using simulated data:", error);
  }

  // Fallback: Generate realistic simulated data
  return generateSimulatedData(symbol, period, currentPrice);
}

// Generate realistic simulated historical data
function generateSimulatedData(symbol, period, currentPrice) {
  const periods = { "1W": 7, "1M": 30, "3M": 90, "6M": 180, "1Y": 365, "ALL": 730 };
  const days = periods[period] || 90;

  // Use symbol hash for consistent random data per symbol
  const symbolHash = symbol.split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  const seedRandom = (seed) => {
    const x = Math.sin(seed) * 10000;
    return x - Math.floor(x);
  };

  // Determine trend based on current performance
  const currentItem = wishlistItems.find(i => i.symbol === symbol);
  const trend = currentItem?.performanceSinceAdded > 0 ? 0.52 : 0.48;

  const volatility = 0.015 + (symbolHash % 10) * 0.002;

  const labels = [];
  const values = [];

  // Start from a calculated historical price
  let price = currentPrice / (1 + ((trend - 0.5) * days * volatility));

  for (let i = days; i >= 0; i--) {
    const date = new Date();
    date.setDate(date.getDate() - i);

    if (period === "1W") {
      labels.push(date.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" }));
    } else if (period === "1M") {
      labels.push(date.toLocaleDateString("en-US", { month: "short", day: "numeric" }));
    } else {
      labels.push(date.toLocaleDateString("en-US", { month: "short", year: "2-digit" }));
    }

    // Generate price movement with trend
    const random = seedRandom(symbolHash + i);
    const change = (random - (1 - trend)) * volatility * price;
    price = Math.max(price + change, price * 0.5);

    // Ensure last price matches current price
    if (i === 0) price = currentPrice;
    values.push(parseFloat(price.toFixed(2)));
  }

  return { labels, values };
}

// ===== Add to Portfolio Modal =====
function openPortfolioModal(itemId) {
  const item = wishlistItems.find(i => i.id === itemId);
  if (!item) return;

  currentAssetData = item;

  document.getElementById("portfolioAssetName").value = `${item.symbol} - ${item.name || item.symbol}`;
  document.getElementById("portfolioAssetSymbol").value = item.symbol;
  document.getElementById("portfolioAssetType").value = item.category || "STOCK";
  document.getElementById("portfolioWishlistItemId").value = item.id;
  document.getElementById("portfolioCurrentPrice").value = item.currentPrice || "";
  document.getElementById("portfolioPurchasePrice").value = item.currentPrice || "";
  document.getElementById("portfolioPurchaseDate").value = new Date().toISOString().split("T")[0];
  document.getElementById("portfolioPurchaseDate").max = new Date().toISOString().split("T")[0];

  document.getElementById("addToPortfolioModal").style.display = "flex";
}

function openAddToPortfolioModal() {
  closeDetailsModal();
  if (currentAssetData) {
    openPortfolioModal(currentAssetData.id);
  }
}

function closePortfolioModal() {
  document.getElementById("addToPortfolioModal").style.display = "none";
  document.getElementById("addToPortfolioForm").reset();
}

async function handleAddToPortfolio(e) {
  e.preventDefault();
  showLoading(true);

  try {
    const assetType = document.getElementById("portfolioAssetType").value;
    const wishlistItemId = document.getElementById("portfolioWishlistItemId").value;

    const assetData = {
      portfolioId: currentPortfolioId,
      assetType: assetType,
      name: currentAssetData?.name || document.getElementById("portfolioAssetSymbol").value,
      symbol: document.getElementById("portfolioAssetSymbol").value,
      quantity: parseFloat(document.getElementById("portfolioQuantity").value),
      purchasePrice: parseFloat(document.getElementById("portfolioPurchasePrice").value),
      currentPrice: parseFloat(document.getElementById("portfolioCurrentPrice").value),
      purchaseDate: document.getElementById("portfolioPurchaseDate").value,
      currency: currentAssetData?.currency || "USD"
    };

    await assetAPI.create(assetData);
    showToast(`${assetData.symbol} added to portfolio!`, "success");

    // Remove from wishlist if checkbox is checked
    if (document.getElementById("removeFromWishlist").checked && wishlistItemId) {
      await wishlistAPI.delete(currentPortfolioId, parseInt(wishlistItemId));
      await loadWishlist();
    }

    closePortfolioModal();
  } catch (error) {
    console.error("Error adding to portfolio:", error);
    showToast("Failed to add to portfolio", "error");
  } finally {
    showLoading(false);
  }
}

// ===== Utility Functions =====
function showLoading(show) {
  const el = document.getElementById("wishlistLoading");
  if (el) el.style.display = show ? "flex" : "none";
}

function debounce(func, wait) {
  let timeout;
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}

// ===== Global Exports =====
window.viewDetails = viewDetails;
window.openPortfolioModal = openPortfolioModal;
window.removeFromWishlist = removeFromWishlist;
window.updateTargetPrice = updateTargetPrice;
window.selectAsset = selectAsset;

// ===== Initialize =====
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initWishlistPage);
} else {
  initWishlistPage();
}

export { initWishlistPage };
