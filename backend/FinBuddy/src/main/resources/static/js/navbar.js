import { portfolioAPI, marketAPI } from "./utils/api.js";
import { debounce } from "./utils/ui.js";

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

    // Fire initial event
    if (select.value) {
      window.dispatchEvent(
        new CustomEvent("portfolioChanged", {
          detail: { portfolioId: select.value },
        }),
      );
    }

    // Setup global search
    setupGlobalSearch();

    // Setup currency selector
    setupCurrencySelector();
  } catch (err) {
    console.error("Navbar initialization failed", err);
  }
}

function setupGlobalSearch() {
  const searchInput = document.getElementById("globalSearchInput");
  const searchResults = document.getElementById("searchResults");

  if (!searchInput || !searchResults) return;

  const performSearch = debounce(async (query) => {
    if (!query || query.length < 2) {
      searchResults.style.display = "none";
      return;
    }

    try {
      searchResults.innerHTML =
        '<div class="search-loading">Searching...</div>';
      searchResults.style.display = "block";

      const results = await marketAPI.searchStocks(query);

      if (results.length === 0) {
        searchResults.innerHTML =
          '<div class="search-empty">No results found</div>';
      } else {
        searchResults.innerHTML = results
          .map(
            (stock) => `
                    <div class="search-result-item" data-symbol="${stock.symbol}">
                        <div class="result-info">
                            <div class="result-symbol">${stock.symbol}</div>
                            <div class="result-name">${stock.name}</div>
                            <div class="result-meta">${stock.exchange} • ${stock.sector}</div>
                        </div>
                        <div class="result-price">
                            ${formatPrice(stock.price, stock.currency)}
                        </div>
                    </div>
                `,
          )
          .join("");

        // Add click handlers
        document.querySelectorAll(".search-result-item").forEach((item) => {
          item.addEventListener("click", () => {
            const symbol = item.dataset.symbol;
            handleSearchResultClick(symbol);
            searchInput.value = "";
            searchResults.style.display = "none";
          });
        });
      }
    } catch (error) {
      console.error("Search error:", error);
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

  // Close search results when clicking outside
  document.addEventListener("click", (e) => {
    if (!searchInput.contains(e.target) && !searchResults.contains(e.target)) {
      searchResults.style.display = "none";
    }
  });
}

function setupCurrencySelector() {
  const currencySelect = document.getElementById("currencySelect");
  if (!currencySelect) return;

  // Set saved currency
  currencySelect.value = currentCurrency;

  currencySelect.addEventListener("change", (e) => {
    currentCurrency = e.target.value;
    localStorage.setItem("preferredCurrency", currentCurrency);

    // Dispatch event to notify other components
    window.dispatchEvent(
      new CustomEvent("currencyChanged", {
        detail: { currency: currentCurrency },
      }),
    );

    // Reload current page data
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

function formatPrice(price, currency) {
  const currencySymbols = {
    INR: "₹",
    USD: "$",
    EUR: "€",
    GBP: "£",
  };

  const symbol = currencySymbols[currency] || currency;
  return `${symbol}${parseFloat(price).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function handleSearchResultClick(symbol) {
  // Show stock details or add to wishlist
  console.log("Selected stock:", symbol);

  // You can add navigation or modal logic here
  window.dispatchEvent(
    new CustomEvent("stockSelected", {
      detail: { symbol },
    }),
  );
}

export function getCurrentCurrency() {
  return currentCurrency;
}
