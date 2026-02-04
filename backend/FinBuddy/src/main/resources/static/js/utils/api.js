// ============================================
// API Module
// Centralized API calls
// ============================================

const API_BASE_URL = "http://localhost:8081/api";

// Generic API call function
async function apiCall(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const config = {
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
    ...options,
  };

  try {
    const response = await fetch(url, config);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error(`API Error [${endpoint}]:`, error);
    throw error;
  }
}

// Portfolio APIs
export const portfolioAPI = {
  getAll: () => apiCall("/portfolios"),
  getById: (id) => apiCall(`/portfolios/${id}`),
  create: (data) =>
    apiCall("/portfolios", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  update: (id, data) =>
    apiCall(`/portfolios/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (id) =>
    apiCall(`/portfolios/${id}`, {
      method: "DELETE",
    }),
  getDashboard: (id) => apiCall(`/portfolios/${id}/dashboard`),
  recalculate: (id) =>
    apiCall(`/portfolios/${id}/recalculate`, {
      method: "POST",
    }),
};

// Asset APIs
export const assetAPI = {
  getByPortfolio: (portfolioId) => apiCall(`/assets/portfolio/${portfolioId}`),
  getById: (id) => apiCall(`/assets/${id}`),
  create: (data) => {
    const typeMap = {
      STOCK: "stocks",
      BOND: "bonds",
      MUTUAL_FUND: "mutualfunds",
      SIP: "sips",
    };

    const endpoint = typeMap[data.assetType];
    if (!endpoint) {
      throw new Error("Unsupported asset type");
    }

    return apiCall(`/assets/${endpoint}/${data.portfolioId}`, {
      method: "POST",
      body: JSON.stringify(data),
    });
  },
  update: (id, data) =>
    apiCall(`/assets/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (id) =>
    apiCall(`/assets/${id}`, {
      method: "DELETE",
    }),
  search: (query) =>
    apiCall(`/assets/search?query=${encodeURIComponent(query)}`),
  updatePrice: (id, price) =>
    apiCall(`/assets/${id}/price`, {
      method: "PATCH",
      body: JSON.stringify({ currentPrice: price }),
    }),
};

// Market Data APIs
export const marketAPI = {
  getStockPrice: (symbol) => apiCall(`/market/stock/${symbol}`),
  getBatchPrices: (symbols) =>
    apiCall("/market/batch-quotes", {
      method: "POST",
      body: JSON.stringify(symbols),
    }),
  getExchangeRate: (from, to) =>
    apiCall(`/market/exchange-rate?from=${from}&to=${to}`),
  getBenchmark: (index) => apiCall(`/market/benchmark/${index}`),
  searchStocks: (query) =>
    apiCall(`/market/search?query=${encodeURIComponent(query)}`),
};

// Report APIs
export const reportAPI = {
  // Fetch PDF for portfolio
  downloadPDF: async (portfolioId) => {
    const url = `${API_BASE_URL}/reports/portfolio/${portfolioId}/pdf`;
    const response = await fetch(url);
    if (!response.ok) throw new Error("Failed to download PDF");
    const blob = await response.blob();
    return blob;
  },

  // Send email with the PDF attached
  sendEmail: async (portfolioId, email) => {
    const response = await fetch(`${API_BASE_URL}/reports/email?portfolioId=${portfolioId}&email=${email}`, {
      method: 'POST',
    });
    return await response.json();  // { success: true/false }
  }
};

export default {
  portfolioAPI,
  assetAPI,
  marketAPI,
  reportAPI,
};
