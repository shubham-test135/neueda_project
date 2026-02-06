// ============================================
// UI Utilities
// ============================================

let cachedRate = 1;
let cachedCurrency = "USD";

export function initUI() {
  setTimeout(() => {
    setupModals();
    setupPortfolioSelector();
  }, 100);
}

// Toast Notifications
export function showToast(message, type = "info") {
  const toast = document.getElementById("toast");
  if (toast) {
    toast.textContent = message;
    toast.className = "toast show";

    const colors = {
      success: "var(--success)",
      error: "var(--danger)",
      warning: "var(--warning)",
      info: "var(--primary)",
    };

    toast.style.borderLeftColor = colors[type] || colors.info;

    setTimeout(() => {
      toast.classList.remove("show");
    }, 3000);
  }
}

// Loading Overlay
export function showLoading() {
  const overlay = document.getElementById("loadingOverlay");
  if (overlay) overlay.style.display = "flex";
}

export function hideLoading() {
  const overlay = document.getElementById("loadingOverlay");
  if (overlay) overlay.style.display = "none";
}

// Modal Management
function setupModals() {
  const portfolioModal = document.getElementById("portfolioModal");
  const createPortfolioBtn = document.getElementById("createPortfolioBtn");
  const closeModalBtn = document.getElementById("closePortfolioModal");
  const closeModalBtns = document.querySelectorAll(".close-modal");

  if (createPortfolioBtn) {
    createPortfolioBtn.addEventListener("click", () => {
      if (portfolioModal) {
        portfolioModal.classList.add("active");
        portfolioModal.style.display = "flex";
      }
    });
  }

  if (closeModalBtn) {
    closeModalBtn.addEventListener("click", () => {
      closeModal(portfolioModal);
    });
  }

  closeModalBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      closeModal(portfolioModal);
    });
  });

  window.addEventListener("click", (e) => {
    if (e.target === portfolioModal) {
      closeModal(portfolioModal);
    }
  });
}

export function closeModal(modal) {
  if (modal) {
    modal.classList.remove("active");
    modal.style.display = "none";
  }
}

export function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add("active");
    modal.style.display = "flex";
  }
}

// Portfolio Selector
function setupPortfolioSelector() {
  const portfolioSelect = document.getElementById("portfolioSelect");
  if (portfolioSelect) {
    portfolioSelect.addEventListener("change", (e) => {
      const portfolioId = e.target.value;
      if (portfolioId) {
        window.dispatchEvent(
            new CustomEvent("portfolioChanged", {
              detail: { portfolioId },
            })
        );
      }
    });
  }
}

// Exchange Rate Updater
export async function updateExchangeRate() {
  const currency = localStorage.getItem("preferredCurrency") || "USD";

  if (currency === cachedCurrency) return;

  if (currency === "USD") {
    cachedRate = 1;
    cachedCurrency = "USD";
    return;
  }

  const response = await fetch(
      `http://localhost:8081/api/market/exchange-rate?from=USD&to=${currency}`
  );
  const data = await response.json();

  cachedRate = data.rate;
  cachedCurrency = currency;
}

// Format Currency (converted + formatted)
export function formatCurrency(amount) {
  const currency = localStorage.getItem("preferredCurrency") || "USD";

  const currencySymbols = {
    INR: "₹",
    USD: "$",
    EUR: "€",
    GBP: "£",
  };

  const symbol = currencySymbols[currency] || currency;
  const value = amount * cachedRate;
  const locale = currency === "USD" ? "en-IN" : "en-US";

  return `${symbol}${value.toLocaleString(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

// Get current currency
export function getCurrentCurrency() {
  return localStorage.getItem("preferredCurrency") || "USD";
}

// Format Number
export function formatNumber(num, decimals = 2) {
  return num.toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Format Percentage
export function formatPercentage(value) {
  const sign = value >= 0 ? "+" : "";
  return `${sign}${value.toFixed(2)}%`;
}

// Debounce Function
export function debounce(func, wait) {
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
