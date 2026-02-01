// ============================================
// Assets Page Logic
// ============================================

import { assetAPI } from "../utils/api.js";
import {
  showToast,
  showLoading,
  hideLoading,
  formatCurrency,
  formatPercentage,
} from "../utils/ui.js";

let currentPortfolioId = null;

async function initAssetsPage() {
  setupEventListeners();

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

  if (toggleAddBtn) {
    toggleAddBtn.addEventListener("click", () => {
      const form = document.getElementById("addAssetForm");
      form.style.display = form.style.display === "none" ? "block" : "none";
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

  const formData = {
    portfolioId: currentPortfolioId,
    assetType: document.getElementById("assetType").value,
    name: document.getElementById("assetName").value,
    symbol: document.getElementById("assetSymbol").value,
    quantity: parseFloat(document.getElementById("assetQuantity").value),
    purchasePrice: parseFloat(document.getElementById("purchasePrice").value),
    currentPrice: parseFloat(document.getElementById("currentPrice").value),
    purchaseDate: document.getElementById("purchaseDate").value,
    currency: document.getElementById("currency").value,
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
