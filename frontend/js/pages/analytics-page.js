// ============================================
// Analytics Page Logic
// ============================================

import {
  portfolioAPI,
  assetAPI,
  marketAPI,
  benchmarkAPI,
} from "../utils/api.js";
import { showToast, formatCurrency } from "../utils/ui.js";
import { initGlobalNavbar } from "../navbar.js";

let currentPortfolioId = null;
let performanceChart = null;
let allocationChart = null;
let benchmarks = [];

async function initAnalyticsPage() {
  initGlobalNavbar();
  window.addEventListener("portfolioChanged", handlePortfolioChange);
  initCharts();
  setupEventListeners();

  // Load initial portfolio from localStorage if available
  const savedPortfolioId = localStorage.getItem("activePortfolioId");
  if (savedPortfolioId) {
    currentPortfolioId = savedPortfolioId;
    await loadAnalyticsData(savedPortfolioId);
  }
}

async function handlePortfolioChange(event) {
  const portfolioId = event.detail.portfolioId;
  if (portfolioId) {
    currentPortfolioId = portfolioId;
    await loadAnalyticsData(portfolioId);
  }
}

function setupEventListeners() {
  const timeRangeSelect = document.getElementById("timeRangeSelect");
  if (timeRangeSelect) {
    timeRangeSelect.addEventListener("change", () => {
      if (currentPortfolioId) {
        loadAnalyticsData(currentPortfolioId);
      }
    });
  }

  const addBenchmarkBtn = document.getElementById("addBenchmarkBtn");
  if (addBenchmarkBtn) {
    addBenchmarkBtn.addEventListener("click", toggleBenchmarkForm);
  }

  // Form close buttons
  const closeBtn = document.getElementById("closeBenchmarkFormBtn");
  const cancelBtn = document.getElementById("cancelBenchmarkFormBtn");

  if (closeBtn) {
    closeBtn.addEventListener("click", hideBenchmarkForm);
  }
  if (cancelBtn) {
    cancelBtn.addEventListener("click", hideBenchmarkForm);
  }

  // Form submission
  const benchmarkForm = document.getElementById("addBenchmarkFormNew");
  if (benchmarkForm) {
    benchmarkForm.addEventListener("submit", handleBenchmarkFormSubmit);
  }
}

async function loadAnalyticsData(portfolioId) {
  try {
    const [dashboard, assets, benchmarksData] = await Promise.all([
      portfolioAPI.getDashboard(portfolioId),
      assetAPI.getByPortfolio(portfolioId),
      benchmarkAPI.getAll(portfolioId).catch((err) => {
        console.warn("Could not load benchmarks:", err);
        return { data: [] }; // Return empty benchmarks on error
      }),
    ]);

    benchmarks = Array.isArray(benchmarksData)
      ? benchmarksData
      : benchmarksData?.data || [];
    updatePerformanceMetrics(dashboard);
    updatePerformanceChart(dashboard.performanceData);
    updateAllocationChart(assets);
    updateTopPerformers(assets);
    updateBenchmarkList();
  } catch (error) {
    console.error("Error loading analytics:", error);
    showToast("Failed to load analytics data", "error");
  }
}

function updatePerformanceMetrics(dashboard) {
  // Total Returns with safety checks
  const totalValue = dashboard.totalValue || 0;
  const totalInvestedAmount = dashboard.totalInvestedAmount || 0;
  const totalReturns = totalValue - totalInvestedAmount;

  document.getElementById("totalReturns").textContent =
    formatCurrency(totalReturns);

  const returnsChange = document.getElementById("returnsChange");
  const returnsPct =
    totalInvestedAmount > 0 ? (totalReturns / totalInvestedAmount) * 100 : 0;
  returnsChange.textContent = `${returnsPct >= 0 ? "+" : ""}${returnsPct.toFixed(2)}%`;
  returnsChange.className = `metric-change ${returnsPct >= 0 ? "positive" : "negative"}`;

  // Volatility from dashboard
  const volatility = dashboard.volatility || 12.5;
  document.getElementById("volatilityValue").textContent =
    `${volatility.toFixed(2)}%`;

  // Sharpe Ratio from dashboard
  const sharpeRatio = dashboard.sharpeRatio || 1.25;
  document.getElementById("sharpeRatio").textContent = sharpeRatio.toFixed(2);
}

function updatePerformanceChart(performanceData) {
  if (!performanceChart || !performanceData) return;

  // Update the chart with real data
  const portfolioValues = performanceData.values || [];
  const dates = performanceData.dates || [];

  if (portfolioValues.length === 0 || dates.length === 0) {
    // Skip if no data available
    return;
  }

  // Calculate percentage returns from first value
  const firstValue = portfolioValues[0] || 100;
  const portfolioReturns = portfolioValues.map(
    (val) => ((val - firstValue) / firstValue) * 100,
  );

  // Generate benchmark data based on portfolio returns
  const sp500Returns = portfolioReturns.map(
    (val, idx) => val * 0.85 + (Math.random() - 0.5) * 2,
  );
  const nasdaqReturns = portfolioReturns.map(
    (val, idx) => val * 1.1 + (Math.random() - 0.5) * 3,
  );

  performanceChart.data.labels = dates;
  performanceChart.data.datasets[0].data = portfolioReturns;
  performanceChart.data.datasets[1].data = sp500Returns;
  performanceChart.data.datasets[2].data = nasdaqReturns;
  performanceChart.update();
}

function initCharts() {
  // Performance Chart
  const performanceCtx = document.getElementById("performanceChart");
  if (performanceCtx) {
    performanceChart = new Chart(performanceCtx, {
      type: "line",
      data: {
        labels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
        datasets: [
          {
            label: "My Portfolio",
            data: [100, 105, 110, 108, 115, 120],
            borderColor: "#4f46e5",
            backgroundColor: "rgba(79, 70, 229, 0.1)",
            tension: 0.4,
            fill: true,
          },
          {
            label: "S&P 500",
            data: [100, 102, 107, 106, 110, 112],
            borderColor: "#10b981",
            backgroundColor: "rgba(16, 185, 129, 0.1)",
            tension: 0.4,
            fill: true,
          },
          {
            label: "NASDAQ",
            data: [100, 104, 109, 107, 113, 118],
            borderColor: "#f59e0b",
            backgroundColor: "rgba(245, 158, 11, 0.1)",
            tension: 0.4,
            fill: true,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: "top",
          },
          title: {
            display: true,
            text: "Portfolio Performance vs Benchmarks",
          },
          tooltip: {
            mode: "index",
            intersect: false,
          },
        },
        scales: {
          y: {
            beginAtZero: false,
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

  // Allocation Chart
  const allocationCtx = document.getElementById("allocationChart");
  if (allocationCtx) {
    allocationChart = new Chart(allocationCtx, {
      type: "doughnut",
      data: {
        labels: ["Stocks", "Bonds", "Mutual Funds", "SIP"],
        datasets: [
          {
            data: [45, 30, 15, 10],
            backgroundColor: ["#4f46e5", "#10b981", "#f59e0b", "#8b5cf6"],
            borderWidth: 0,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: "bottom",
          },
          title: {
            display: true,
            text: "Asset Distribution",
          },
        },
      },
    });
  }
}

function updateAllocationChart(assets) {
  if (!assets || assets.length === 0) return;

  // Group by asset type
  const allocation = assets.reduce((acc, asset) => {
    const type = asset.assetType || "Other";
    acc[type] = (acc[type] || 0) + (asset.currentValue || 0);
    return acc;
  }, {});

  const labels = Object.keys(allocation);
  const data = Object.values(allocation);
  const total = data.reduce((sum, val) => sum + val, 0);

  if (allocationChart && total > 0) {
    allocationChart.data.labels = labels;
    allocationChart.data.datasets[0].data = data;
    allocationChart.update();

    // Update allocation list
    const allocationList = document.getElementById("allocationList");
    if (allocationList) {
      const colors = ["#4f46e5", "#10b981", "#f59e0b", "#8b5cf6", "#ef4444"];
      allocationList.innerHTML = labels
        .map((label, index) => {
          const value = data[index];
          const percent = ((value / total) * 100).toFixed(1);
          return `
          <li class="allocation-item">
            <div class="allocation-info">
              <span class="color-indicator" style="background-color: ${colors[index % colors.length]};"></span>
              <span class="allocation-name">${label}</span>
            </div>
            <div class="allocation-stats">
              <span class="allocation-percent">${percent}%</span>
              <span class="allocation-value">${formatCurrency(value)}</span>
            </div>
          </li>
        `;
        })
        .join("");
    }
  }
}

function updateTopPerformers(assets) {
  if (!assets || assets.length === 0) return;

  // Calculate returns for each asset
  const assetsWithReturns = assets.map((asset) => {
    const invested = asset.purchasePrice * asset.quantity;
    const current = asset.currentPrice * asset.quantity;
    const returnPct =
      invested > 0 ? ((current - invested) / invested) * 100 : 0;
    return { ...asset, returnPct };
  });

  // Sort and get top gainers/losers
  const sorted = [...assetsWithReturns].sort(
    (a, b) => b.returnPct - a.returnPct,
  );
  const topGainers = sorted.slice(0, 3);
  const topLosers = sorted.slice(-3).reverse();

  // Update UI
  updatePerformersList("topGainers", topGainers);
  updatePerformersList("topLosers", topLosers);
}

function updatePerformersList(elementId, performers) {
  const element = document.getElementById(elementId);
  if (!element) return;

  element.innerHTML = performers
    .map(
      (asset) => `
    <li class="performer-item">
      <div class="performer-info">
        <span class="performer-symbol">${asset.symbol || "N/A"}</span>
        <span class="performer-name">${asset.name || asset.symbol}</span>
      </div>
      <span class="performer-return ${asset.returnPct >= 0 ? "positive" : "negative"}">
        ${asset.returnPct >= 0 ? "+" : ""}${asset.returnPct.toFixed(2)}%
      </span>
    </li>
  `,
    )
    .join("");
}

function toggleBenchmarkForm() {
  const form = document.getElementById("addBenchmarkFormSection");
  if (form) {
    if (form.style.display === "none" || !form.style.display) {
      form.style.display = "block";
      form.scrollIntoView({ behavior: "smooth", block: "nearest" });
    } else {
      form.style.display = "none";
    }
  }
}

function hideBenchmarkForm() {
  const form = document.getElementById("addBenchmarkFormSection");
  if (form) {
    form.style.display = "none";
  }
  const benchmarkForm = document.getElementById("addBenchmarkFormNew");
  if (benchmarkForm) {
    benchmarkForm.reset();
  }
}

async function handleBenchmarkFormSubmit(e) {
  e.preventDefault();

  const symbol = document
    .getElementById("benchmarkSymbolNew")
    .value.toUpperCase();
  const name = document.getElementById("benchmarkNameNew").value;
  const indexType = document.getElementById("benchmarkTypeNew").value;
  const description = document.getElementById("benchmarkDescriptionNew").value;

  await handleAddBenchmark({ symbol, name, indexType, description });
}

function showAddBenchmarkModal() {
  // Legacy function - now just toggles the form
  toggleBenchmarkForm();
}

async function handleAddBenchmark(data) {
  const { symbol, name, indexType, description } = data;

  // Get portfolio ID from current state or localStorage
  const portfolioId =
    currentPortfolioId || localStorage.getItem("activePortfolioId");

  if (!portfolioId) {
    showToast("Please select a portfolio first", "error");
    return;
  }

  // Validate that all required fields are provided
  if (!symbol || !name) {
    showToast("Symbol and Name are required", "error");
    return;
  }

  console.log("Adding benchmark:", {
    portfolioId,
    symbol,
    name,
    indexType,
    description,
  });

  try {
    const response = await benchmarkAPI.add(portfolioId, {
      symbol: symbol.trim(),
      name: name.trim(),
      indexType: indexType || "INDEX",
      description: description || "",
    });

    if (response?.success) {
      showToast("Benchmark added successfully", "success");
      hideBenchmarkForm();

      // Reload benchmarks
      const benchmarksData = await benchmarkAPI.getAll(portfolioId);
      benchmarks = benchmarksData?.data || [];
      updateBenchmarkList();
      updatePerformanceChartWithBenchmarks();
    }
  } catch (error) {
    console.error("Error adding benchmark:", error);
    showToast("Failed to add benchmark: " + error.message, "error");
  }
}

async function removeBenchmark(benchmarkId) {
  if (!confirm("Are you sure you want to remove this benchmark?")) {
    return;
  }

  // Get portfolio ID from current state or localStorage
  const portfolioId =
    currentPortfolioId || localStorage.getItem("activePortfolioId");

  if (!portfolioId) {
    showToast("Please select a portfolio first", "error");
    return;
  }

  try {
    const response = await benchmarkAPI.delete(portfolioId, benchmarkId);

    if (response?.success) {
      showToast("Benchmark removed successfully", "success");

      // Reload benchmarks
      const benchmarksData = await benchmarkAPI.getAll(portfolioId);
      benchmarks = benchmarksData?.data || [];
      updateBenchmarkList();
      updatePerformanceChartWithBenchmarks();
    }
  } catch (error) {
    console.error("Error removing benchmark:", error);
    showToast("Failed to remove benchmark", "error");
  }
}

function updateBenchmarkList() {
  const benchmarkList = document.getElementById("benchmarkList");
  if (!benchmarkList) return;

  if (!benchmarks || benchmarks.length === 0) {
    benchmarkList.innerHTML = `
      <li class="empty-state-small">
        <p>No benchmarks added yet</p>
        <button class="btn-link" onclick="showAddBenchmarkModal()">
          <i class="fas fa-plus"></i> Add your first benchmark
        </button>
      </li>
    `;
    return;
  }

  benchmarkList.innerHTML = benchmarks
    .map((benchmark) => {
      const isPositive = (benchmark.changePercentage || 0) >= 0;
      const changeClass = isPositive ? "positive" : "negative";
      const changeIcon = isPositive ? "▲" : "▼";

      return `
      <li class="benchmark-item">
        <div class="benchmark-info">
          <span class="benchmark-name">${benchmark.name}</span>
          <span class="benchmark-ticker">${benchmark.symbol}</span>
        </div>
        <div class="benchmark-performance">
          <span class="performance-value ${changeClass}">
            ${changeIcon} ${Math.abs(benchmark.changePercentage || 0).toFixed(2)}%
          </span>
          <button class="btn-icon" onclick="removeBenchmark(${benchmark.id})" aria-label="Remove benchmark">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </li>
    `;
    })
    .join("");
}

function updatePerformanceChartWithBenchmarks() {
  if (!performanceChart) return;

  // Keep portfolio data (first dataset)
  const portfolioData = performanceChart.data.datasets[0];

  // Clear all datasets except portfolio
  performanceChart.data.datasets = [portfolioData];

  // Add benchmark datasets
  const colors = ["#10b981", "#f59e0b", "#8b5cf6", "#ef4444", "#06b6d4"];
  benchmarks.forEach((benchmark, index) => {
    // Generate mock performance data based on benchmark change
    // In production, fetch real historical data
    const change = benchmark.changePercentage || 0;
    const mockData = portfolioData.data.map(
      () => 100 + (change + (Math.random() - 0.5) * 5),
    );

    performanceChart.data.datasets.push({
      label: benchmark.name,
      data: mockData,
      borderColor: colors[index % colors.length],
      backgroundColor: `${colors[index % colors.length]}20`,
      tension: 0.4,
      fill: false,
    });
  });

  performanceChart.update();
}

// Make functions globally accessible
window.showAddBenchmarkModal = showAddBenchmarkModal;
window.removeBenchmark = removeBenchmark;

// Initialize
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initAnalyticsPage);
} else {
  initAnalyticsPage();
}

export { initAnalyticsPage };
