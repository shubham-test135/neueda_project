// ============================================
// Assets Page Logic
// ============================================

import { assetAPI, portfolioAPI } from "../utils/api.js";
import { initGlobalNavbar, getCurrentCurrency } from "../navbar.js";

import {
  showToast,
  showLoading,
  hideLoading,
  formatCurrency,
  formatPercentage,
  debounce,
} from "../utils/ui.js";

let currentPortfolioId = null;
const FINNHUB_API_KEY = "d5vg7hpr01qjj9jjd8k0d5vg7hpr01qjj9jjd8kg";

async function initAssetsPage() {
  initGlobalNavbar();
  setupEventListeners();

  // Check if there's a prefilled symbol from wishlist
  const prefillSymbol = sessionStorage.getItem("prefill_symbol");
  if (prefillSymbol) {
    // Clear the session storage
    sessionStorage.removeItem("prefill_symbol");

    // Open the add asset form
    const form = document.getElementById("addAssetForm");
    if (form) {
      form.style.display = "block";

      // Wait a bit for the form to be visible, then trigger search
      setTimeout(() => {
        const assetNameInput = document.getElementById("assetName");
        if (assetNameInput) {
          assetNameInput.value = prefillSymbol;
          // Trigger the fuzzy search
          searchStockSymbol(prefillSymbol);
        }
      }, 100);
    }
  }

  // Listen for portfolio changes
  window.addEventListener("portfolioChanged", handlePortfolioChange);
}

function setupEventListeners() {
  const toggleAddBtn = document.getElementById("toggleAddAssetBtn");
  const cancelBtns = document.querySelectorAll(
    "#cancelAssetBtn, #cancelAssetBtn2",
  );
  const assetForm = document.getElementById("assetForm");
  const searchBtn = document.getElementById("searchBtn");
  const assetSearch = document.getElementById("assetSearch");
  const assetNameInput = document.getElementById("assetName");
  const purchaseDateInput = document.getElementById("purchaseDate");
  const currencySelect = document.getElementById("currency");

  if (toggleAddBtn) {
    toggleAddBtn.addEventListener("click", () => {
      const form = document.getElementById("addAssetForm");
      form.style.display = form.style.display === "none" ? "block" : "none";

      // Set currency from global setting when opening form
      if (form.style.display === "block" && currencySelect) {
        const globalCurrency = getCurrentCurrency();
        currencySelect.value = globalCurrency;
      }
    });
  }

  cancelBtns.forEach((btn) => {
    if (btn) {
      btn.addEventListener("click", () => {
        document.getElementById("addAssetForm").style.display = "none";
      });
    }
  });

  if (assetForm) {
    assetForm.addEventListener("submit", handleCreateAsset);
  }

  if (searchBtn && assetSearch) {
    searchBtn.addEventListener("click", () => handleSearch(assetSearch.value));
    assetSearch.addEventListener("keypress", (e) => {
      if (e.key === "Enter") {
        handleSearch(assetSearch.value);
      }
    });
  }

  // Set up fuzzy search for asset name
  if (assetNameInput) {
    const debouncedSearch = debounce(searchStockSymbol, 300);
    assetNameInput.addEventListener("input", (e) => {
      debouncedSearch(e.target.value);
    });

    // Close search results when clicking outside
    document.addEventListener("click", (e) => {
      const searchResults = document.getElementById("assetSearchResults");
      if (
        searchResults &&
        !assetNameInput.contains(e.target) &&
        !searchResults.contains(e.target)
      ) {
        searchResults.style.display = "none";
      }
    });
  }

  // Set max date to today for purchase date
  if (purchaseDateInput) {
    const today = new Date().toISOString().split("T")[0];
    purchaseDateInput.setAttribute("max", today);
  }
}

async function handlePortfolioChange(event) {
  const portfolioId = event.detail.portfolioId;
  if (portfolioId) {
    currentPortfolioId = portfolioId;
    await loadAssets(portfolioId);
  }
}

async function loadAssets(portfolioId) {
  showLoading();
  try {
    const assets = await assetAPI.getByPortfolio(portfolioId);
    renderAssets(assets);
    hideLoading();
  } catch (error) {
    console.error("Error loading assets:", error);
    showToast("Failed to load assets", "error");
    hideLoading();
  }
}

function renderAssets(assets) {
  const tbody = document.getElementById("assetsTableBody");
  if (!tbody) return;

  if (assets.length === 0) {
    tbody.innerHTML = `
            <tr>
                <td colspan="9" class="empty-state-row">
                    <div class="empty-state">
                        <i class="fas fa-folder-open"></i>
                        <p>No assets found. Add your first asset!</p>
                    </div>
                </td>
            </tr>
        `;
    return;
  }

  tbody.innerHTML = assets
    .map(
      (asset) => `
        <tr>
            <td>${asset.name}</td>
            <td>${asset.symbol}</td>
            <td><span class="badge badge-primary">${asset.assetType}</span></td>
            <td>${asset.quantity}</td>
            <td>${formatCurrency(asset.currentPrice || 0, asset.currency)}</td>
            <td>${formatCurrency(asset.currentValue || 0, asset.currency)}</td>
            <td class="${(asset.gainLoss || 0) >= 0 ? "positive" : "negative"}">
                ${formatCurrency(asset.gainLoss || 0, asset.currency)}
            </td>
            <td class="${(asset.gainLossPercentage || 0) >= 0 ? "positive" : "negative"}">
                ${formatPercentage(asset.gainLossPercentage || 0)}
            </td>
            <td>
                <button class="chart-btn" onclick="editAsset(${asset.id})" title="Edit">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="chart-btn" onclick="deleteAsset(${asset.id})" title="Delete">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    `,
    )
    .join("");
}

async function handleCreateAsset(e) {
  e.preventDefault();

  if (!currentPortfolioId) {
    showToast("Please select a portfolio first", "warning");
    return;
  }

  // Use global currency setting
  const globalCurrency = getCurrentCurrency();

  const formData = {
    portfolioId: currentPortfolioId,
    assetType: document.getElementById("assetType").value,
    name: document.getElementById("assetName").value,
    symbol: document.getElementById("assetSymbol").value,
    quantity: parseFloat(document.getElementById("assetQuantity").value),
    purchasePrice: parseFloat(document.getElementById("purchasePrice").value),
    currentPrice: parseFloat(document.getElementById("currentPrice").value),
    purchaseDate: document.getElementById("purchaseDate").value,
    currency: globalCurrency,
    isWishlist: document.getElementById("isWishlist").checked,
  };

  showLoading();
  try {
    await assetAPI.create(formData);
    showToast("Asset added successfully", "success");

    // Reset and hide form
    e.target.reset();
    document.getElementById("addAssetForm").style.display = "none";

    // Reload assets
    await loadAssets(currentPortfolioId);
    hideLoading();
  } catch (error) {
    console.error("Error creating asset:", error);
    showToast("Failed to add asset", "error");
    hideLoading();
  }
}

async function handleSearch(query) {
  if (!query.trim()) {
    if (currentPortfolioId) {
      await loadAssets(currentPortfolioId);
    }
    return;
  }

  showLoading();
  try {
    const assets = await assetAPI.search(query);
    renderAssets(assets);
    hideLoading();
  } catch (error) {
    console.error("Error searching assets:", error);
    showToast("Search failed", "error");
    hideLoading();
  }
}

// Fuzzy search for stock symbols using Finnhub API
async function searchStockSymbol(query) {
  const searchResults = document.getElementById("assetSearchResults");

  if (!query || query.length < 2) {
    searchResults.style.display = "none";
    return;
  }

  try {
    const url = `https://finnhub.io/api/v1/search?q=${encodeURIComponent(query)}&token=${FINNHUB_API_KEY}`;
    const response = await fetch(url);
    const data = await response.json();

    if (!data.result || data.result.length === 0) {
      searchResults.innerHTML =
        '<div class="search-item">No results found</div>';
      searchResults.style.display = "block";
      return;
    }

    searchResults.innerHTML = data.result
      .slice(0, 10)
      .map(
        (item) => `
        <div class="search-item" data-symbol="${item.symbol}" data-name="${item.description}">
          <div class="search-item-symbol">${item.symbol}</div>
          <div class="search-item-name">${item.description}</div>
          <div class="search-item-type">${item.type}</div>
        </div>
      `,
      )
      .join("");

    searchResults.style.display = "block";

    // Add click handlers to search results
    searchResults.querySelectorAll(".search-item").forEach((item) => {
      item.addEventListener("click", async () => {
        const symbol = item.dataset.symbol;
        const name = item.dataset.name;

        // Set name and symbol
        document.getElementById("assetName").value = name;
        document.getElementById("assetSymbol").value = symbol;

        // Fetch and set current price
        await fetchStockPrice(symbol);

        // Hide search results
        searchResults.style.display = "none";
      });
    });
  } catch (error) {
    console.error("Error searching stocks:", error);
    searchResults.innerHTML = '<div class="search-item">Search failed</div>';
    searchResults.style.display = "block";
  }
}

// Fetch current stock price using Finnhub API
async function fetchStockPrice(symbol) {
  try {
    const url = `https://finnhub.io/api/v1/quote?symbol=${symbol}&token=${FINNHUB_API_KEY}`;
    const response = await fetch(url);
    const data = await response.json();

    if (data.c && data.c > 0) {
      document.getElementById("currentPrice").value = data.c.toFixed(2);
    }
  } catch (error) {
    console.error("Error fetching stock price:", error);
  }
}

// Global functions
window.editAsset = async function (id) {
  console.log("Edit asset:", id);
  // TODO: Implement edit functionality
};

window.deleteAsset = async function (id) {
  if (confirm("Are you sure you want to delete this asset?")) {
    showLoading();
    try {
      await assetAPI.delete(id);
      showToast("Asset deleted successfully", "success");
      if (currentPortfolioId) {
        await loadAssets(currentPortfolioId);
      }
      hideLoading();
    } catch (error) {
      console.error("Error deleting asset:", error);
      showToast("Failed to delete asset", "error");
      hideLoading();
    }
  }
};

// Initialize
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initAssetsPage);
} else {
  initAssetsPage();
}

export { initAssetsPage };
