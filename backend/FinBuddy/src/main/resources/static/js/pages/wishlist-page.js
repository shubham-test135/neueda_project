// ============================================
// Wishlist Page Logic
// ============================================

import { marketAPI } from "../utils/api.js";
import { showToast, formatCurrency } from "../utils/ui.js";
import { initGlobalNavbar } from "../navbar.js";

let wishlistItems = [];

async function initWishlistPage() {
  initGlobalNavbar();
  loadWishlist();
  setupEventListeners();
}

function setupEventListeners() {
  // Add to wishlist button
  const addBtn = document.getElementById("addToWishlistBtn");
  const addFirstBtn = document.getElementById("addFirstItem");
  if (addBtn) {
    addBtn.addEventListener("click", showAddWishlistModal);
  }
  if (addFirstBtn) {
    addFirstBtn.addEventListener("click", showAddWishlistModal);
  }

  // Modal controls
  const modal = document.getElementById("addWishlistModal");
  const closeBtn = modal?.querySelector(".modal-close");
  const cancelBtn = document.getElementById("cancelWishlistBtn");
  const form = document.getElementById("addWishlistForm");

  if (closeBtn) {
    closeBtn.addEventListener("click", closeAddWishlistModal);
  }
  if (cancelBtn) {
    cancelBtn.addEventListener("click", closeAddWishlistModal);
  }
  if (form) {
    form.addEventListener("submit", handleAddToWishlist);
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
}

function loadWishlist() {
  // Load from localStorage (in production, this would be from API)
  const stored = localStorage.getItem("finbuddy_wishlist");
  if (stored) {
    wishlistItems = JSON.parse(stored);
  } else {
    // Mock data for demo
    wishlistItems = [
      {
        id: 1,
        symbol: "AAPL",
        name: "Apple Inc.",
        category: "stocks",
        currentPrice: 185.25,
        change: 2.35,
        changePercent: 2.35,
        dayRange: "$182.10 - $186.50",
        yearRange: "$142.50 - $195.80",
        marketCap: "$2.85T",
        targetPrice: 200,
        dateAdded: new Date().toISOString(),
      },
      {
        id: 2,
        symbol: "GOOGL",
        name: "Alphabet Inc.",
        category: "stocks",
        currentPrice: 142.8,
        change: -1.25,
        changePercent: -1.25,
        dayRange: "$141.20 - $144.50",
        yearRange: "$102.30 - $155.20",
        marketCap: "$1.78T",
        targetPrice: null,
        dateAdded: new Date().toISOString(),
      },
    ];
  }

  updateWishlistSummary();
  renderWishlist();
}

function updateWishlistSummary() {
  document.getElementById("totalWatchlist").textContent = wishlistItems.length;

  const gainers = wishlistItems.filter((item) => item.changePercent > 0).length;
  const losers = wishlistItems.filter((item) => item.changePercent < 0).length;
  const alerts = wishlistItems.filter((item) => item.targetPrice).length;

  document.getElementById("gainersCount").textContent = gainers;
  document.getElementById("losersCount").textContent = losers;
  document.getElementById("alertsCount").textContent = alerts;
}

function renderWishlist() {
  const grid = document.getElementById("wishlistGrid");
  const empty = document.getElementById("emptyWishlist");

  if (wishlistItems.length === 0) {
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
          <p class="asset-name">${item.name}</p>
          <span class="asset-category">${item.category}</span>
        </div>
        <button class="btn-icon favorite active" onclick="removeFromWishlist(${item.id})" aria-label="Remove from wishlist">
          <i class="fas fa-star"></i>
        </button>
      </div>

      <div class="wishlist-card-body">
        <div class="price-info">
          <p class="current-price">${formatCurrency(item.currentPrice)}</p>
          <span class="price-change ${item.changePercent >= 0 ? "positive" : "negative"}">
            <i class="fas fa-arrow-${item.changePercent >= 0 ? "up" : "down"}"></i>
            ${item.changePercent >= 0 ? "+" : ""}${item.changePercent.toFixed(2)}%
          </span>
        </div>

        <div class="price-details">
          <div class="detail-item">
            <span class="detail-label">Day Range</span>
            <span class="detail-value">${item.dayRange}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">52W Range</span>
            <span class="detail-value">${item.yearRange}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Market Cap</span>
            <span class="detail-value">${item.marketCap}</span>
          </div>
        </div>

        <div class="alert-section">
          <div class="alert-input">
            <label>
              <i class="fas fa-bell"></i> Price Alert
            </label>
            <input
              type="number"
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

function showAddWishlistModal() {
  const modal = document.getElementById("addWishlistModal");
  if (modal) {
    modal.showModal();
  }
}

function closeAddWishlistModal() {
  const modal = document.getElementById("addWishlistModal");
  const form = document.getElementById("addWishlistForm");
  if (modal) {
    modal.close();
  }
  if (form) {
    form.reset();
  }
}

async function handleAddToWishlist(event) {
  event.preventDefault();

  const symbol = document.getElementById("symbolInput").value.toUpperCase();
  const category = document.getElementById("categoryInput").value;
  const targetPrice = document.getElementById("targetPriceInput").value;
  const notes = document.getElementById("notesInput").value;

  // Check if already in wishlist
  if (wishlistItems.some((item) => item.symbol === symbol)) {
    showToast(`${symbol} is already in your wishlist`, "warning");
    return;
  }

  try {
    // Fetch current price
    const priceData = await marketAPI.getStockPrice(symbol);

    const newItem = {
      id: Date.now(),
      symbol,
      name: `${symbol} Inc.`, // In production, get from API
      category,
      currentPrice: priceData.price || 0,
      change: 0,
      changePercent: 0,
      dayRange: "N/A",
      yearRange: "N/A",
      marketCap: "N/A",
      targetPrice: targetPrice ? parseFloat(targetPrice) : null,
      notes,
      dateAdded: new Date().toISOString(),
    };

    wishlistItems.push(newItem);
    saveWishlist();
    renderWishlist();
    updateWishlistSummary();
    closeAddWishlistModal();
    showToast(`${symbol} added to wishlist`, "success");
  } catch (error) {
    console.error("Error adding to wishlist:", error);
    showToast("Failed to add to wishlist", "error");
  }
}

function removeFromWishlist(id) {
  wishlistItems = wishlistItems.filter((item) => item.id !== id);
  saveWishlist();
  renderWishlist();
  updateWishlistSummary();
  showToast("Removed from wishlist", "success");
}

function updateTargetPrice(id, price) {
  const item = wishlistItems.find((i) => i.id === id);
  if (item) {
    item.targetPrice = price ? parseFloat(price) : null;
    saveWishlist();
    updateWishlistSummary();
    showToast("Price alert updated", "success");
  }
}

function saveWishlist() {
  localStorage.setItem("finbuddy_wishlist", JSON.stringify(wishlistItems));
}

function filterWishlist() {
  const search = document.getElementById("wishlistSearch").value.toLowerCase();
  const category = document.getElementById("categoryFilter").value;

  let filtered = wishlistItems;

  if (search) {
    filtered = filtered.filter(
      (item) =>
        item.symbol.toLowerCase().includes(search) ||
        item.name.toLowerCase().includes(search),
    );
  }

  if (category && category !== "all") {
    filtered = filtered.filter((item) => item.category === category);
  }

  // Re-render with filtered items
  const grid = document.getElementById("wishlistGrid");
  // Implementation similar to renderWishlist but with filtered array
}

function sortWishlist() {
  const sortBy = document.getElementById("sortBy").value;

  switch (sortBy) {
    case "name":
      wishlistItems.sort((a, b) => a.symbol.localeCompare(b.symbol));
      break;
    case "price":
      wishlistItems.sort((a, b) => b.currentPrice - a.currentPrice);
      break;
    case "change":
      wishlistItems.sort((a, b) => b.changePercent - a.changePercent);
      break;
    case "added":
      wishlistItems.sort(
        (a, b) => new Date(b.dateAdded) - new Date(a.dateAdded),
      );
      break;
  }

  renderWishlist();
}

function viewDetails(symbol) {
  showToast(`Viewing details for ${symbol}`, "info");
  // Navigate to details page or show modal
}

function addToPortfolio(symbol) {
  showToast(`Add ${symbol} to portfolio feature coming soon!`, "info");
  // Navigate to add asset page
}

// Make functions global for onclick handlers
window.removeFromWishlist = removeFromWishlist;
window.updateTargetPrice = updateTargetPrice;
window.viewDetails = viewDetails;
window.addToPortfolio = addToPortfolio;

// Initialize
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initWishlistPage);
} else {
  initWishlistPage();
}

export { initWishlistPage };
