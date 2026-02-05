// ============================================
// Main Application Entry Point
// Handles component loading and initialization
// ============================================

import { loadComponents } from "./utils/component-loader.js";
import { initNavigation } from "./utils/navigation.js";
import { initTheme } from "./utils/theme.js";
import { initUI } from "./utils/ui.js";
import { initAiChat } from "./ai-chat.js";


// Initialize app when DOM is ready
document.addEventListener("DOMContentLoaded", async () => {
  try {
    // Load all components
    await loadComponents();

    // Initialize modules
    initNavigation();
    initTheme();
    initUI();
    initAiChat();

    console.log("✅ FinBuddy initialized successfully");
  } catch (error) {
    console.error("❌ Error initializing app:", error);
  }
});

// Export global utilities
export { showToast, showLoading, hideLoading } from "./utils/ui.js";
export { refreshPortfolioDropdown } from "./navbar.js";

// Make refreshPortfolioDropdown available globally
import { refreshPortfolioDropdown } from "./navbar.js";
window.refreshPortfolioDropdown = refreshPortfolioDropdown;
