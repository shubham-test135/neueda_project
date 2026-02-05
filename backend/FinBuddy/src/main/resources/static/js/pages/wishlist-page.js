// ============================================
// Wishlist Page Logic - Backend Integrated
// ============================================

import { marketAPI, wishlistAPI, portfolioAPI } from "../utils/api.js";
import { showToast, formatCurrency } from "../utils/ui.js";
import { initGlobalNavbar } from "../navbar.js";

let wishlistItems = [];
let currentPortfolioId = 1; // Default portfolio ID

/**
 * Initialize Wishlist Page
 */
async function initWishlistPage() {
  initGlobalNavbar();

  // Get portfolio ID from session storage or default
  const storedPortfolioId = sessionStorage.getItem("current_portfolio_id");
  if (storedPortfolioId) {
    currentPortfolioId = parseInt(storedPortfolioId);
  }

  await loadWishlist();
  setupEventListeners();
}

/**
 * Setup Event Listeners
 */
function setupEventListeners() {
  // Add to wishlist button
  const addBtn = document.getElementById("addToWishlistBtn");
  const addFirstBtn = document.getElementById("addFirstItem");

  if (addBtn) {
    addBtn.addEventListener("click", toggleAddWishlistForm);
  }
  if (addFirstBtn) {
    addFirstBtn.addEventListener("click", toggleAddWishlistForm);
  }

  // Form close buttons
  const cancelBtn = document.getElementById("cancelWishlistBtn");
  const cancelBtn2 = document.getElementById("cancelWishlistBtn2");

  if (cancelBtn) {
    cancelBtn.addEventListener("click", hideAddWishlistForm);
  }
  if (cancelBtn2) {
    cancelBtn2.addEventListener("click", hideAddWishlistForm);
  }

  // Form submission
  const wishlistForm = document.getElementById("wishlistForm");
  if (wishlistForm) {
    wishlistForm.addEventListener("submit", handleWishlistFormSubmit);
  }

  // Set up search for asset name
  const wishlistNameInput = document.getElementById("wishlistAssetName");
  if (wishlistNameInput) {
    const debouncedSearch = debounce(searchAssets, 300);
    wishlistNameInput.addEventListener("input", (e) => {
      debouncedSearch(e.target.value);
    });

    // Close search results when clicking outside
    document.addEventListener("click", (e) => {
      const searchResults = document.getElementById("wishlistSearchResults");
      if (
        searchResults &&
        !wishlistNameInput.contains(e.target) &&
        !searchResults.contains(e.target)
      ) {
        searchResults.style.display = "none";
      }
    });
  }

  // Search and filter
  const searchInput = document.getElementById("wishlistSearch");
  const categoryFilter = document.getElementById("categoryFilter");
  const sortBy = document.getElementById("sortBy");

  if (searchInput) {
    searchInput.addEventListener("input", filterWishlist);
  }
  if (categoryFilter) {
    categoryFilter.addEventListener("change", filterWishlist);
  }
  if (sortBy) {
    sortBy.addEventListener("change", sortWishlist);
  }

  // Refresh prices button
  const refreshBtn = document.getElementById("refreshPricesBtn");
  if (refreshBtn) {
    refreshBtn.addEventListener("click", refreshPrices);
  }
}

/**
 * Toggle Add Wishlist Form
 */
function toggleAddWishlistForm() {
  const form = document.getElementById("addWishlistForm");
  if (form) {
    if (form.style.display === "none" || !form.style.display) {
      form.style.display = "block";
      form.scrollIntoView({ behavior: "smooth", block: "nearest" });
    } else {
      form.style.display = "none";
    }
  }
}

/**
 * Hide Add Wishlist Form
 */
function hideAddWishlistForm() {
  const form = document.getElementById("addWishlistForm");
  if (form) {
    form.style.display = "none";
  }
  // Reset form
  const wishlistForm = document.getElementById("wishlistForm");
  if (wishlistForm) {
    wishlistForm.reset();
  }
}

/**
 * Handle Wishlist Form Submission
 */
async function handleWishlistFormSubmit(e) {
  e.preventDefault();

  const formData = {
    symbol: document.getElementById("wishlistAssetSymbol").value.toUpperCase(),
    name: document.getElementById("wishlistAssetName").value,
    category: document.getElementById("wishlistAssetType").value,
    targetPrice: document.getElementById("wishlistTargetPrice").value || null,
  };

  await handleAddToWishlist(formData);
}

/**
 * Load Wishlist from Backend
 */
async function loadWishlist() {
  try {
    showLoading(true);

    // Fetch wishlist from backend
    const response = await wishlistAPI.getAll(currentPortfolioId);

    if (response && response.success) {
      wishlistItems = response.data || [];
    } else {
      wishlistItems = [];
    }

    await updateWishlistSummary();
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

/**
 * Update Wishlist Summary from Backend
 */
async function updateWishlistSummary() {
  try {
    const response = await wishlistAPI.getSummary(currentPortfolioId);

    if (response && response.success) {
      const summary = response.data;
      document.getElementById("totalWatchlist").textContent =
        summary.totalWatchlist || 0;
      document.getElementById("gainersCount").textContent =
        summary.gainersCount || 0;
      document.getElementById("losersCount").textContent =
        summary.losersCount || 0;
      document.getElementById("alertsCount").textContent =
        summary.alertsCount || 0;
    }
  } catch (error) {
    console.error("Error updating summary:", error);
    // Fallback to local calculation
    document.getElementById("totalWatchlist").textContent =
      wishlistItems.length;
    const gainers = wishlistItems.filter(
      (item) => (item.changePercentage || 0) > 0,
    ).length;
    const losers = wishlistItems.filter(
      (item) => (item.changePercentage || 0) < 0,
    ).length;
    const alerts = wishlistItems.filter((item) => item.alertEnabled).length;
    document.getElementById("gainersCount").textContent = gainers;
    document.getElementById("losersCount").textContent = losers;
    document.getElementById("alertsCount").textContent = alerts;
  }
}

/**
 * Render Wishlist Items
 */
function renderWishlist() {
  const grid = document.getElementById("wishlistGrid");
  const empty = document.getElementById("emptyWishlist");

  if (!wishlistItems || wishlistItems.length === 0) {
    grid.style.display = "none";
    empty.style.display = "flex";
    return;
  }

  grid.style.display = "grid";
  empty.style.display = "none";

  grid.innerHTML = wishlistItems
    .map(
      (item) => `
    <article class="wishlist-card" data-id="${item.id}">
      <div class="wishlist-card-header">
        <div class="asset-info">
          <h3 class="asset-symbol">${item.symbol}</h3>
          <p class="asset-name">${item.name || item.symbol}</p>
          <span class="asset-category">${item.category || "STOCK"}</span>
        </div>
        <button class="btn-icon favorite active" onclick="removeFromWishlist(${item.id})" aria-label="Remove from wishlist">
          <i class="fas fa-star"></i>
        </button>
      </div>

      <div class="wishlist-card-body">
        <div class="price-info">
          <p class="current-price">${formatCurrency(item.currentPrice || 0)}</p>
          <span class="price-change ${(item.changePercentage || 0) >= 0 ? "positive" : "negative"}">
            <i class="fas fa-arrow-${(item.changePercentage || 0) >= 0 ? "up" : "down"}"></i>
            ${(item.changePercentage || 0) >= 0 ? "+" : ""}${(item.changePercentage || 0).toFixed(2)}%
          </span>
        </div>

        <div class="price-details">
          <div class="detail-item">
            <span class="detail-label">Added Price</span>
            <span class="detail-value">${formatCurrency(item.priceWhenAdded || 0)}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Performance</span>
            <span class="detail-value ${(item.performanceSinceAdded || 0) >= 0 ? "positive" : "negative"}">
              ${(item.performanceSinceAdded || 0) >= 0 ? "+" : ""}${(item.performanceSinceAdded || 0).toFixed(2)}%
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Currency</span>
            <span class="detail-value">${item.currency || "USD"}</span>
          </div>
        </div>

        <div class="alert-section">
          <div class="alert-input">
            <label>
              <i class="fas fa-bell ${item.alertEnabled ? "text-primary" : ""}"></i> Price Alert
              ${item.alertTriggered ? '<span class="badge badge-danger">Triggered!</span>' : ""}
            </label>
            <input
              type="number"
              step="0.01"
              placeholder="Set alert price"
              class="input-field"
              value="${item.targetPrice || ""}"
              onchange="updateTargetPrice(${item.id}, this.value)"
              aria-label="Set price alert for ${item.symbol}"
            />
          </div>
        </div>
      </div>

      <div class="wishlist-card-footer">
        <button class="btn-secondary btn-small" onclick="viewDetails('${item.symbol}')">
          <i class="fas fa-chart-line"></i> View Details
        </button>
        <button class="btn-primary btn-small" onclick="addToPortfolio('${item.symbol}')">
          <i class="fas fa-shopping-cart"></i> Add to Portfolio
        </button>
      </div>
    </article>
  `,
    )
    .join("");
}

/**
 * Handle Add to Wishlist
 */ async function handleAddToWishlist(formData) {
  const symbol = (formData.symbol || "").toUpperCase();
  const category = (formData.category || "STOCK").toUpperCase();
  const targetPrice = formData.targetPrice;

  try {
    showLoading(true);

    const requestData = {
      symbol: symbol,
      category: category,
      targetPrice: targetPrice || null,
    };

    const response = await wishlistAPI.add(currentPortfolioId, requestData);

    if (response && response.success) {
      await loadWishlist();
      showToast(response.message || `${symbol} added to wishlist`, "success");
      hideAddWishlistForm(); // Close form on success
    } else {
      throw new Error(response.message || "Failed to add to wishlist");
    }
  } catch (error) {
    console.error("Error adding to wishlist:", error);
    const errorMessage = error.message || "Failed to add to wishlist";
    showToast(errorMessage, "error");
    // Don't re-throw - keep form open so user can retry
  } finally {
    showLoading(false);
  }
}

async function removeFromWishlist(itemId) {
  if (
    !confirm("Are you sure you want to remove this item from your wishlist?")
  ) {
    return;
  }

  try {
    showLoading(true);

    const response = await wishlistAPI.delete(currentPortfolioId, itemId);

    if (response && response.success) {
      await loadWishlist();
      showToast(response.message || "Removed from wishlist", "success");
    } else {
      throw new Error(response.message || "Failed to remove from wishlist");
    }
  } catch (error) {
    console.error("Error removing from wishlist:", error);
    showToast(error.message || "Failed to remove from wishlist", "error");
  } finally {
    showLoading(false);
  }
}

async function updateTargetPrice(itemId, price) {
  try {
    const updates = {
      targetPrice: price ? parseFloat(price) : null,
    };

    const response = await wishlistAPI.update(
      currentPortfolioId,
      itemId,
      updates,
    );

    if (response && response.success) {
      const item = wishlistItems.find((i) => i.id === itemId);
      if (item) {
        item.targetPrice = updates.targetPrice;
        item.alertEnabled = updates.targetPrice !== null;
      }

      await updateWishlistSummary();
      showToast(response.message || "Price alert updated", "success");
    } else {
      throw new Error(response.message || "Failed to update price alert");
    }
  } catch (error) {
    console.error("Error updating target price:", error);
    showToast(error.message || "Failed to update price alert", "error");
  }
}

async function refreshPrices() {
  try {
    showLoading(true);

    const response = await wishlistAPI.refreshPrices(currentPortfolioId);

    if (response && response.success) {
      wishlistItems = response.data || [];
      renderWishlist();
      await updateWishlistSummary();
      showToast(response.message || "Prices refreshed successfully", "success");
    } else {
      throw new Error(response.message || "Failed to refresh prices");
    }
  } catch (error) {
    console.error("Error refreshing prices:", error);
    showToast(error.message || "Failed to refresh prices", "error");
  } finally {
    showLoading(false);
  }
}

function filterWishlist() {
  const search = document.getElementById("wishlistSearch").value.toLowerCase();
  const category = document.getElementById("categoryFilter").value;

  let filtered = [...wishlistItems];

  if (search) {
    filtered = filtered.filter(
      (item) =>
        item.symbol.toLowerCase().includes(search) ||
        (item.name && item.name.toLowerCase().includes(search)),
    );
  }

  if (category && category !== "all") {
    filtered = filtered.filter(
      (item) => (item.category || "").toLowerCase() === category.toLowerCase(),
    );
  }

  const originalItems = wishlistItems;
  wishlistItems = filtered;
  renderWishlist();
  wishlistItems = originalItems;
}

function sortWishlist() {
  const sortBy = document.getElementById("sortBy").value;

  switch (sortBy) {
    case "name":
      wishlistItems.sort((a, b) => a.symbol.localeCompare(b.symbol));
      break;
    case "price":
      wishlistItems.sort(
        (a, b) => (b.currentPrice || 0) - (a.currentPrice || 0),
      );
      break;
    case "change":
      wishlistItems.sort(
        (a, b) => (b.changePercentage || 0) - (a.changePercentage || 0),
      );
      break;
    case "added":
      wishlistItems.sort((a, b) => new Date(b.addedAt) - new Date(a.addedAt));
      break;
  }

  renderWishlist();
}

async function viewDetails(symbol) {
  const modal = document.getElementById("stockDetailsModal");
  const loading = modal?.querySelector(".stock-details-loading");
  const content = modal?.querySelector(".stock-details-content");
  const error = modal?.querySelector(".stock-details-error");

  if (!modal) return;

  setupModalCloseHandlers(modal);

  modal.style.display = "flex";
  modal.classList.add("active");
  if (loading) loading.style.display = "block";
  if (content) content.style.display = "none";
  if (error) error.style.display = "none";

  try {
    const data = await marketAPI.getStockPrice(symbol);

    document.getElementById("stockSymbol").textContent = data.symbol || symbol;

    const stockInfo = wishlistItems.find((item) => item.symbol === symbol);
    document.getElementById("stockDescription").textContent =
      stockInfo?.name || data.name || "Stock Information";

    document.getElementById("stockPrice").textContent = formatCurrency(
      data.currentPrice || data.price || 0,
    );
    document.getElementById("stockPreviousClose").textContent = formatCurrency(
      data.previousClose || 0,
    );

    const change = data.changeAmount || data.change || 0;
    const changePercent = data.changePercentage || data.changePercent || 0;
    const changeColor =
      change >= 0 ? "var(--success-color)" : "var(--danger-color)";
    const changeIcon = change >= 0 ? "▲" : "▼";

    document.getElementById("stockChange").innerHTML =
      `<span style="color: ${changeColor}">${changeIcon} ${formatCurrency(Math.abs(change))}</span>`;
    document.getElementById("stockChangePercent").innerHTML =
      `<span style="color: ${changeColor}">${changeIcon} ${Math.abs(changePercent).toFixed(2)}%</span>`;

    if (loading) loading.style.display = "none";
    if (content) content.style.display = "block";
  } catch (err) {
    console.error("Failed to load stock details:", err);
    if (loading) loading.style.display = "none";
    if (error) error.style.display = "block";
  }
}

function setupModalCloseHandlers(modal) {
  if (!modal || modal.dataset.closeHandlersSetup) return;
  modal.dataset.closeHandlersSetup = "true";

  const closeBtn = document.getElementById("closeStockDetailsModal");

  if (closeBtn) {
    closeBtn.onclick = () => {
      modal.style.display = "none";
      modal.classList.remove("active");
    };
  }

  modal.onclick = (e) => {
    if (e.target === modal) {
      modal.style.display = "none";
      modal.classList.remove("active");
    }
  };

  modal.querySelectorAll(".close-modal").forEach((btn) => {
    btn.onclick = () => {
      modal.style.display = "none";
      modal.classList.remove("active");
    };
  });
}

function addToPortfolio(symbol) {
  sessionStorage.setItem("prefill_symbol", symbol);
  window.location.href = "/pages/assets.html";
}

function showLoading(show) {
  const loadingEl = document.getElementById("wishlistLoading");
  if (loadingEl) {
    loadingEl.style.display = show ? "block" : "none";
  }
}

/**
 * Add trending asset to wishlist
 */
async function addTrendingToWishlist(symbol, name, category) {
  try {
    showLoading(true);

    const requestData = {
      symbol: symbol,
      category: category || "STOCK",
      targetPrice: null,
    };

    const response = await wishlistAPI.add(currentPortfolioId, requestData);

    if (response && response.success) {
      await loadWishlist();
      showToast(`${symbol} added to wishlist`, "success");

      // Update the star icon to filled
      const trendingCards = document.querySelectorAll(".trending-card");
      trendingCards.forEach((card) => {
        const btn = card.querySelector(`button[onclick*="${symbol}"]`);
        if (btn) {
          const icon = btn.querySelector("i");
          if (icon) {
            icon.classList.remove("far");
            icon.classList.add("fas");
          }
          btn.disabled = true;
          btn.style.opacity = "0.6";
        }
      });
    } else {
      throw new Error(response.message || "Failed to add to wishlist");
    }
  } catch (error) {
    console.error("Error adding trending to wishlist:", error);
    showToast(error.message || "Failed to add to wishlist", "error");
  } finally {
    showLoading(false);
  }
}

/**
 * Debounce function for search
 */
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

/**
 * Search for assets (stocks, bonds, mutual funds, SIP)
 * Uses multiple free APIs based on asset type
 */
async function searchAssets(query) {
  const searchResults = document.getElementById("wishlistSearchResults");
  const assetType = document.getElementById("wishlistAssetType").value;

  if (!query || query.length < 2) {
    searchResults.style.display = "none";
    return;
  }

  try {
    let results = [];

    // Use different APIs based on asset type
    if (assetType === "STOCK" || !assetType) {
      // Use Finnhub for stocks (free tier: 60 API calls/minute)
      results = await searchStocks(query);
    } else if (assetType === "MUTUAL_FUND" || assetType === "SIP") {
      // Use RapidAPI's Indian Mutual Fund API or MFApi
      results = await searchMutualFunds(query);
    } else if (assetType === "BOND") {
      // For bonds, use a generic search or provide common Indian bonds
      results = await searchBonds(query);
    }

    if (!results || results.length === 0) {
      searchResults.innerHTML =
        '<div class="search-item">No results found</div>';
      searchResults.style.display = "block";
      return;
    }

    searchResults.innerHTML = results
      .slice(0, 10)
      .map(
        (item) => `
        <div class="search-item" data-symbol="${item.symbol}" data-name="${item.name}" data-type="${item.type}">
          <div class="search-item-symbol">${item.symbol}</div>
          <div class="search-item-name">${item.name}</div>
          <div class="search-item-type">${item.type}</div>
        </div>
      `,
      )
      .join("");

    searchResults.style.display = "block";

    // Add click handlers to search results
    searchResults.querySelectorAll(".search-item").forEach((item) => {
      item.addEventListener("click", () => {
        const symbol = item.dataset.symbol;
        const name = item.dataset.name;

        // Set name and symbol
        document.getElementById("wishlistAssetName").value = name;
        document.getElementById("wishlistAssetSymbol").value = symbol;

        // Hide search results
        searchResults.style.display = "none";
      });
    });
  } catch (error) {
    console.error("Error searching assets:", error);
    searchResults.innerHTML = '<div class="search-item">Search failed</div>';
    searchResults.style.display = "block";
  }
}

/**
 * Search for stocks using Finnhub API
 */
async function searchStocks(query) {
  const FINNHUB_API_KEY = "ct5r409r01qncqetq900ct5r409r01qncqetq90g"; // Free tier

  try {
    const url = `https://finnhub.io/api/v1/search?q=${encodeURIComponent(query)}&token=${FINNHUB_API_KEY}`;
    const response = await fetch(url);
    const data = await response.json();

    if (!data.result || data.result.length === 0) {
      return [];
    }

    return data.result.map((item) => ({
      symbol: item.symbol,
      name: item.description,
      type: item.type || "Stock",
    }));
  } catch (error) {
    console.error("Error searching stocks:", error);
    return [];
  }
}

/**
 * Search for Indian mutual funds
 * Using MFApi.in - Free Indian Mutual Fund API
 */
async function searchMutualFunds(query) {
  try {
    // MFApi provides a list of all mutual funds - we'll filter locally
    const url = "https://api.mfapi.in/mf";
    const response = await fetch(url);
    const data = await response.json();

    if (!Array.isArray(data)) {
      return [];
    }

    // Filter mutual funds by query
    const filtered = data
      .filter((fund) =>
        fund.schemeName.toLowerCase().includes(query.toLowerCase()),
      )
      .slice(0, 20);

    return filtered.map((fund) => ({
      symbol: fund.schemeCode.toString(),
      name: fund.schemeName,
      type: "Mutual Fund",
    }));
  } catch (error) {
    console.error("Error searching mutual funds:", error);
    // Fallback to some common Indian mutual funds
    return getCommonMutualFunds(query);
  }
}

/**
 * Search for bonds - providing common Indian bonds
 */
async function searchBonds(query) {
  // Common Indian government and corporate bonds
  const commonBonds = [
    {
      symbol: "INDBOND-10Y",
      name: "India 10 Year Government Bond",
      type: "Government Bond",
    },
    {
      symbol: "INDBOND-5Y",
      name: "India 5 Year Government Bond",
      type: "Government Bond",
    },
    {
      symbol: "INDBOND-3Y",
      name: "India 3 Year Government Bond",
      type: "Government Bond",
    },
    {
      symbol: "HDFC-BOND",
      name: "HDFC Corporate Bond",
      type: "Corporate Bond",
    },
    {
      symbol: "ICICI-BOND",
      name: "ICICI Corporate Bond",
      type: "Corporate Bond",
    },
    { symbol: "SBI-BOND", name: "SBI Corporate Bond", type: "Corporate Bond" },
    {
      symbol: "TATA-BOND",
      name: "Tata Corporate Bond",
      type: "Corporate Bond",
    },
    {
      symbol: "RELIANCE-BOND",
      name: "Reliance Corporate Bond",
      type: "Corporate Bond",
    },
  ];

  return commonBonds.filter(
    (bond) =>
      bond.name.toLowerCase().includes(query.toLowerCase()) ||
      bond.symbol.toLowerCase().includes(query.toLowerCase()),
  );
}

/**
 * Fallback common mutual funds for when API fails
 */
function getCommonMutualFunds(query) {
  const commonFunds = [
    {
      symbol: "120503",
      name: "HDFC Balanced Advantage Fund",
      type: "Mutual Fund",
    },
    { symbol: "118989", name: "SBI Bluechip Fund", type: "Mutual Fund" },
    {
      symbol: "120716",
      name: "ICICI Prudential Bluechip Fund",
      type: "Mutual Fund",
    },
    { symbol: "119551", name: "Axis Bluechip Fund", type: "Mutual Fund" },
    {
      symbol: "120828",
      name: "Mirae Asset Large Cap Fund",
      type: "Mutual Fund",
    },
    {
      symbol: "119591",
      name: "Parag Parikh Flexi Cap Fund",
      type: "Mutual Fund",
    },
    {
      symbol: "120836",
      name: "Kotak Standard Multicap Fund",
      type: "Mutual Fund",
    },
  ];

  return commonFunds.filter((fund) =>
    fund.name.toLowerCase().includes(query.toLowerCase()),
  );
}

// Global functions
window.removeFromWishlist = removeFromWishlist;
window.updateTargetPrice = updateTargetPrice;
window.viewDetails = viewDetails;
window.addToPortfolio = addToPortfolio;
window.addTrendingToWishlist = addTrendingToWishlist;

// Initialize
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initWishlistPage);
} else {
  initWishlistPage();
}

export { initWishlistPage };
