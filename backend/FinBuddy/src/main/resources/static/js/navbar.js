import { portfolioAPI, marketAPI } from "./utils/api.js";
import { debounce, formatCurrency, updateExchangeRate , showLoading ,hideLoading } from "./utils/ui.js";

let currentCurrency = localStorage.getItem("preferredCurrency") || "INR";

export async function initGlobalNavbar() {
  const waitForNavbar = () =>
    new Promise((resolve) => {
      const check = () => {
        const select = document.getElementById("portfolioSelect");
        if (select) resolve(select);
        else setTimeout(check, 50);
      };
      check();
    });

  const select = await waitForNavbar();
  const refreshBtn = document.getElementById("refreshDataBtn");
  if (refreshBtn) {
    refreshBtn.addEventListener("click", handleGlobalRefresh);
  }

  try {
    const portfolios = await portfolioAPI.getAll();
    select.innerHTML = '<option value="">Select Portfolio</option>';

    portfolios.forEach((p) => {
      const option = document.createElement("option");
      option.value = p.id;
      option.textContent = p.name;
      select.appendChild(option);
    });

    const saved = localStorage.getItem("activePortfolioId");

    if (saved && portfolios.some((p) => p.id === saved)) {
      select.value = saved;
    } else if (portfolios.length > 0) {
      select.value = portfolios[0].id;
      localStorage.setItem("activePortfolioId", portfolios[0].id);
    }

    select.addEventListener("change", () => {
      const id = select.value;
      localStorage.setItem("activePortfolioId", id);
      window.dispatchEvent(
        new CustomEvent("portfolioChanged", { detail: { portfolioId: id } }),
      );
    });

    if (select.value) {
      window.dispatchEvent(
        new CustomEvent("portfolioChanged", {
          detail: { portfolioId: select.value },
        }),
      );
    }

    setupGlobalSearch();
    setupCurrencySelector();
  } catch (err) {
    console.error("Navbar initialization failed", err);
  }
}

async function handleGlobalRefresh() {
  const portfolioId = localStorage.getItem("activePortfolioId");

  if (!portfolioId) {
    alert("Please select a portfolio before refreshing.");
    return;
  }

  try {
    showLoading();
    const refreshBtn = document.getElementById("refreshDataBtn");
    if (refreshBtn) refreshBtn.disabled = true;

    await portfolioAPI.recalculate(portfolioId);

    hideLoading();
    window.dispatchEvent(
      new CustomEvent("portfolioChanged", {
        detail: { portfolioId },
      }),
    );
  } catch (err) {
    console.error("Refresh failed:", err);
    alert("Failed to refresh portfolio data. Please try again.");
  } finally {
    const refreshBtn = document.getElementById("refreshDataBtn");
    if (refreshBtn) refreshBtn.disabled = false;
  }
}

function setupGlobalSearch() {
  const searchInput = document.getElementById("globalSearchInput");
  const searchResults = document.getElementById("searchResults");

  if (!searchInput || !searchResults) return;

  const FINNHUB_API_KEY = "d5vg7hpr01qjj9jjd8k0d5vg7hpr01qjj9jjd8kg";

  const performSearch = debounce(async (query) => {
    if (!query || query.length < 2) {
      searchResults.style.display = "none";
      return;
    }

    try {
      searchResults.innerHTML =
        '<div class="search-loading">Searching...</div>';
      searchResults.style.display = "block";

      // Use Finnhub API for fuzzy stock search
      const url = `https://finnhub.io/api/v1/search?q=${encodeURIComponent(query)}&token=${FINNHUB_API_KEY}`;
      const response = await fetch(url);
      const data = await response.json();

      if (!data.result || data.result.length === 0) {
        searchResults.innerHTML =
          '<div class="search-empty">No results found</div>';
      } else {
        searchResults.innerHTML = data.result
          .slice(0, 10) // Limit to top 10 results
          .map(
            (item) => `
              <div class="search-result-item" data-symbol="${item.symbol}">
                <div class="result-info">
                  <div class="result-symbol">${item.symbol}</div>
                  <div class="result-name">${item.description}</div>
                  <div class="result-meta">${item.type}</div>
                </div>
              </div>
            `,
          )
          .join("");

        document.querySelectorAll(".search-result-item").forEach((item) => {
          item.addEventListener("click", () => {
            const symbol = item.dataset.symbol;
            handleSearchResultClick(symbol);
            searchInput.value = "";
            searchResults.style.display = "none";
          });
        });
      }
    } catch (err) {
      console.error("Search error:", err);
      searchResults.innerHTML =
        '<div class="search-error">Search failed. Please try again.</div>';
    }
  }, 300);

  searchInput.addEventListener("input", (e) => {
    performSearch(e.target.value);
  });

  searchInput.addEventListener("focus", (e) => {
    if (e.target.value.length >= 2) {
      searchResults.style.display = "block";
    }
  });

  document.addEventListener("click", (e) => {
    if (!searchInput.contains(e.target) && !searchResults.contains(e.target)) {
      searchResults.style.display = "none";
    }
  });

  window.addEventListener("currencyChanged", () => {
    const query = searchInput.value;
    if (query && query.length >= 2) {
      performSearch(query);
    }
  });
}

function setupCurrencySelector() {
  const currencySelect = document.getElementById("currencySelect");
  if (!currencySelect) return;

  currencySelect.value = currentCurrency;

  currencySelect.addEventListener("change", async (e) => {
    currentCurrency = e.target.value;
    localStorage.setItem("preferredCurrency", currentCurrency);

    await updateExchangeRate();

    window.dispatchEvent(
      new CustomEvent("currencyChanged", {
        detail: { currency: currentCurrency },
      }),
    );

    const portfolioId = localStorage.getItem("activePortfolioId");
    if (portfolioId) {
      window.dispatchEvent(
        new CustomEvent("portfolioChanged", {
          detail: { portfolioId },
        }),
      );
    }
  });
}

function handleSearchResultClick(symbol) {
  // Show stock details modal
  showStockDetailsModal(symbol);
}

async function showStockDetailsModal(symbol) {
  const modal = document.getElementById("stockDetailsModal");
  const loading = modal.querySelector(".stock-details-loading");
  const content = modal.querySelector(".stock-details-content");
  const error = modal.querySelector(".stock-details-error");

  // Setup close handlers
  setupModalCloseHandlers(modal);

  // Show modal and loading state
  modal.style.display = "flex";
  modal.classList.add("active");
  loading.style.display = "block";
  content.style.display = "none";
  error.style.display = "none";

  try {
    // Fetch stock details from backend
    const response = await fetch(`/api/market/quote/${symbol}`);
    const data = await response.json();

    // Update modal content
    document.getElementById("stockSymbol").textContent = data.symbol || symbol;

    // Fetch additional details from Finnhub for description
    const FINNHUB_API_KEY = "d5vg7hpr01qjj9jjd8k0d5vg7hpr01qjj9jjd8kg";
    const searchUrl = `https://finnhub.io/api/v1/search?q=${encodeURIComponent(symbol)}&token=${FINNHUB_API_KEY}`;
    const searchResponse = await fetch(searchUrl);
    const searchData = await searchResponse.json();

    const stockInfo = searchData.result?.find((r) => r.symbol === symbol);
    document.getElementById("stockDescription").textContent =
      stockInfo?.description || "Stock Information";

    document.getElementById("stockPrice").textContent = formatCurrency(
      data.currentPrice,
    );
    document.getElementById("stockPreviousClose").textContent = formatCurrency(
      data.previousClose,
    );

    const change = data.change || 0;
    const changePercent = data.changePercent || 0;
    const changeColor =
      change >= 0 ? "var(--success-color)" : "var(--danger-color)";
    const changeIcon = change >= 0 ? "▲" : "▼";

    document.getElementById("stockChange").innerHTML =
      `<span style="color: ${changeColor}">${changeIcon} ${formatCurrency(Math.abs(change))}</span>`;
    document.getElementById("stockChangePercent").innerHTML =
      `<span style="color: ${changeColor}">${changeIcon} ${Math.abs(changePercent).toFixed(2)}%</span>`;

    // Hide loading, show content
    loading.style.display = "none";
    content.style.display = "block";
  } catch (err) {
    console.error("Failed to load stock details:", err);
    loading.style.display = "none";
    error.style.display = "block";
  }
}

// Setup modal close handlers
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

  // Close modal when clicking outside
  modal.onclick = (e) => {
    if (e.target === modal) {
      modal.style.display = "none";
      modal.classList.remove("active");
    }
  };

  // Close modal with close-modal class buttons
  modal.querySelectorAll(".close-modal").forEach((btn) => {
    btn.onclick = () => {
      modal.style.display = "none";
      modal.classList.remove("active");
    };
  });
}

export function getCurrentCurrency() {
  return currentCurrency;
}

export async function refreshPortfolioDropdown() {
  const select = document.getElementById("portfolioSelect");
  if (!select) return;

  try {
    const portfolios = await portfolioAPI.getAll();
    const currentValue = select.value;

    select.innerHTML = '<option value="">Select Portfolio</option>';

    portfolios.forEach((p) => {
      const option = document.createElement("option");
      option.value = p.id;
      option.textContent = p.name;
      select.appendChild(option);
    });

    // Restore selected value or select the newest portfolio
    if (currentValue && portfolios.some((p) => p.id === currentValue)) {
      select.value = currentValue;
    } else if (portfolios.length > 0) {
      const lastPortfolio = portfolios[portfolios.length - 1];
      select.value = lastPortfolio.id;
      localStorage.setItem("activePortfolioId", lastPortfolio.id);
    }
  } catch (err) {
    console.error("Failed to refresh portfolio dropdown:", err);
  }
}
