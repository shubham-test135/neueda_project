// ============================================
// Analytics Page Logic
// ============================================

import { portfolioAPI, assetAPI, marketAPI } from "../utils/api.js";
import { showToast, formatCurrency } from "../utils/ui.js";
import { initGlobalNavbar } from "../navbar.js";

let currentPortfolioId = null;
let performanceChart = null;
let allocationChart = null;

async function initAnalyticsPage() {
  initGlobalNavbar();
  window.addEventListener("portfolioChanged", handlePortfolioChange);
  initCharts();
  setupEventListeners();
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
    addBenchmarkBtn.addEventListener("click", showAddBenchmarkModal);
  }
}

async function loadAnalyticsData(portfolioId) {
  try {
    const [dashboard, assets] = await Promise.all([
      portfolioAPI.getDashboard(portfolioId),
      assetAPI.getByPortfolio(portfolioId),
    ]);

    updatePerformanceMetrics(dashboard);
    updatePerformanceChart(dashboard.performanceData);
    updateAllocationChart(assets);
    updateTopPerformers(assets);
    updateRiskMetrics(dashboard);
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

  // CAGR (mock calculation - should be from backend)
  const cagr = calculateCAGR(totalInvestedAmount, totalValue, 1);
  document.getElementById("cagrValue").textContent = `${cagr.toFixed(2)}%`;

  // Volatility (mock)
  document.getElementById("volatilityValue").textContent = "12.5%";

  // Sharpe Ratio (mock)
  document.getElementById("sharpeRatio").textContent = "1.25";
}

function calculateCAGR(initialValue, finalValue, years) {
  if (initialValue <= 0 || years <= 0) return 0;
  return (Math.pow(finalValue / initialValue, 1 / years) - 1) * 100;
}

function updatePerformanceChart(performanceData) {
  if (!performanceChart || !performanceData) return;

  // Update the chart with real data
  const portfolioValues = performanceData.values || [];
  const dates = performanceData.dates || [];

  // Calculate percentage returns from first value
  const firstValue = portfolioValues[0] || 100;
  const portfolioReturns = portfolioValues.map(
    (val) => ((val - firstValue) / firstValue) * 100,
  );

  // Generate mock benchmark data based on portfolio returns (in production, fetch real benchmark data)
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

function updateRiskMetrics(dashboard) {
  // These are mock values - should be calculated on backend
  document.getElementById("betaValue").textContent = "1.05";
  document.getElementById("maxDrawdown").textContent = "-12.4%";
  document.getElementById("varValue").textContent = formatCurrency(2450);
}

function showAddBenchmarkModal() {
  showToast("Add Benchmark feature coming soon!", "info");
}

// Initialize
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initAnalyticsPage);
} else {
  initAnalyticsPage();
}

export { initAnalyticsPage };
