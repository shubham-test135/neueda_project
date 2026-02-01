// ============================================
// Dashboard Page Logic
// ============================================

import { portfolioAPI, assetAPI, marketAPI } from "../utils/api.js";
import {
  showToast,
  showLoading,
  hideLoading,
  formatCurrency,
  formatPercentage,
} from "../utils/ui.js";

let currentPortfolioId = null;
let charts = {};

// Initialize dashboard
async function initDashboard() {
  setupEventListeners();
  await loadPortfolios();

  // Listen for portfolio changes
  window.addEventListener("portfolioChanged", handlePortfolioChange);
}

function setupEventListeners() {
  const refreshBtn = document.getElementById("refreshDataBtn");
  if (refreshBtn) {
    refreshBtn.addEventListener("click", () => {
      if (currentPortfolioId) {
        loadDashboardData(currentPortfolioId);
      }
    });
  }
}

async function loadPortfolios() {
  try {
    const portfolios = await portfolioAPI.getAll();
    updatePortfolioSelector(portfolios);

    if (portfolios.length > 0) {
      currentPortfolioId = portfolios[0].id;
      document.getElementById("portfolioSelect").value = currentPortfolioId;
      await loadDashboardData(currentPortfolioId);
    }
  } catch (error) {
    console.error("Error loading portfolios:", error);
    showToast("Failed to load portfolios", "error");
  }
}

function updatePortfolioSelector(portfolios) {
  const select = document.getElementById("portfolioSelect");
  if (select) {
    select.innerHTML = '<option value="">Select Portfolio</option>';
    portfolios.forEach((portfolio) => {
      const option = document.createElement("option");
      option.value = portfolio.id;
      option.textContent = portfolio.name;
      select.appendChild(option);
    });
  }
}

async function handlePortfolioChange(event) {
  const portfolioId = event.detail.portfolioId;
  if (portfolioId) {
    currentPortfolioId = portfolioId;
    await loadDashboardData(portfolioId);
  }
}

async function loadDashboardData(portfolioId) {
  showLoading();
  try {
    const dashboard = await portfolioAPI.getDashboard(portfolioId);
    updateSummaryCards(dashboard);
    updateCharts(dashboard);
    updateBenchmarks(portfolioId);
    hideLoading();
  } catch (error) {
    console.error("Error loading dashboard:", error);
    showToast("Failed to load dashboard data", "error");
    hideLoading();
  }
}

function updateSummaryCards(dashboard) {
  document.getElementById("totalValue").textContent = formatCurrency(
    dashboard.totalValue,
  );
  document.getElementById("totalInvestment").textContent = formatCurrency(
    dashboard.totalInvestment,
  );
  document.getElementById("gainLoss").textContent = formatCurrency(
    dashboard.totalGainLoss,
  );

  const gainLossPercent = document.getElementById("gainLossPercent");
  const gainLossPercentValue = dashboard.gainLossPercentage || 0;
  gainLossPercent.innerHTML = `
        <i class="fas fa-arrow-${gainLossPercentValue >= 0 ? "up" : "down"}"></i> 
        ${formatPercentage(gainLossPercentValue)}
    `;
  gainLossPercent.className = `card-percentage ${gainLossPercentValue >= 0 ? "positive" : "negative"}`;

  document.getElementById("assetCount").textContent =
    dashboard.totalAssets || 0;
}

function updateCharts(dashboard) {
  if (dashboard.assetAllocation) {
    renderAssetAllocationChart(dashboard.assetAllocation);
  }
  if (dashboard.performanceData) {
    renderPerformanceChart(dashboard.performanceData);
  }
  if (dashboard.topPerformers) {
    renderTopPerformersChart(dashboard.topPerformers);
  }
}

function renderAssetAllocationChart(data) {
  const ctx = document.getElementById("assetAllocationChart");
  if (!ctx) return;

  // Destroy existing chart
  if (charts.allocation) {
    charts.allocation.destroy();
  }

  charts.allocation = new Chart(ctx, {
    type: "doughnut",
    data: {
      labels: data.map((d) => d.assetType),
      datasets: [
        {
          data: data.map((d) => d.percentage),
          backgroundColor: [
            "rgba(79, 70, 229, 0.8)",
            "rgba(168, 85, 247, 0.8)",
            "rgba(236, 72, 153, 0.8)",
            "rgba(245, 158, 11, 0.8)",
          ],
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: "bottom",
        },
      },
    },
  });
}

function renderPerformanceChart(data) {
  const ctx = document.getElementById("portfolioGrowthChart");
  if (!ctx) return;

  if (charts.performance) {
    charts.performance.destroy();
  }

  charts.performance = new Chart(ctx, {
    type: "line",
    data: {
      labels: data.dates || [],
      datasets: [
        {
          label: "Portfolio Value",
          data: data.values || [],
          borderColor: "rgba(79, 70, 229, 1)",
          backgroundColor: "rgba(79, 70, 229, 0.1)",
          tension: 0.4,
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      },
    },
  });
}

function renderTopPerformersChart(data) {
  const ctx = document.getElementById("assetPerformanceChart");
  if (!ctx) return;

  if (charts.topPerformers) {
    charts.topPerformers.destroy();
  }

  charts.topPerformers = new Chart(ctx, {
    type: "bar",
    data: {
      labels: data.map((d) => d.name),
      datasets: [
        {
          label: "Gain/Loss %",
          data: data.map((d) => d.gainLossPercentage),
          backgroundColor: data.map((d) =>
            d.gainLossPercentage >= 0
              ? "rgba(16, 185, 129, 0.8)"
              : "rgba(239, 68, 68, 0.8)",
          ),
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      },
    },
  });
}

async function updateBenchmarks(portfolioId) {
  try {
    const [sp500, nifty] = await Promise.all([
      marketAPI.getBenchmark("SP500"),
      marketAPI.getBenchmark("NIFTY50"),
    ]);

    document.getElementById("sp500Value").textContent = formatCurrency(
      sp500.value,
    );
    document.getElementById("nifty50Value").textContent = formatCurrency(
      nifty.value,
    );
  } catch (error) {
    console.error("Error loading benchmarks:", error);
  }
}

// Initialize when module loads
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initDashboard);
} else {
  initDashboard();
}

export { initDashboard };
