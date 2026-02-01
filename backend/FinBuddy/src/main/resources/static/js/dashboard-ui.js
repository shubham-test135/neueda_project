// ============================================
// FinBuddy Dashboard UI Enhancements
// Sidebar, Navbar, and Navigation Interactions
// ============================================

// Wait for DOM to load
document.addEventListener("DOMContentLoaded", function () {
  initializeDashboardUI();
});

function initializeDashboardUI() {
  // Sidebar Toggle
  const sidebar = document.getElementById("sidebar");
  const sidebarToggle = document.getElementById("sidebarToggle");
  const mobileMenuBtn = document.getElementById("mobileMenuBtn");

  if (sidebarToggle) {
    sidebarToggle.addEventListener("click", function () {
      sidebar.classList.toggle("collapsed");
      localStorage.setItem(
        "sidebarCollapsed",
        sidebar.classList.contains("collapsed"),
      );
    });
  }

  if (mobileMenuBtn) {
    mobileMenuBtn.addEventListener("click", function () {
      sidebar.classList.toggle("active");
    });
  }

  // Restore sidebar state
  const sidebarCollapsed = localStorage.getItem("sidebarCollapsed") === "true";
  if (sidebarCollapsed && window.innerWidth > 1024) {
    sidebar.classList.add("collapsed");
  }

  // Navigation
  const navItems = document.querySelectorAll(".nav-item");
  navItems.forEach((item) => {
    item.addEventListener("click", function (e) {
      e.preventDefault();

      // Remove active class from all items
      navItems.forEach((nav) => nav.classList.remove("active"));

      // Add active class to clicked item
      this.classList.add("active");

      // Get page name
      const page = this.getAttribute("data-page");
      handleNavigation(page);

      // Close mobile menu
      if (window.innerWidth <= 1024) {
        sidebar.classList.remove("active");
      }
    });
  });

  // Theme Toggle
  const themeToggle = document.getElementById("themeToggle");
  if (themeToggle) {
    themeToggle.addEventListener("click", function () {
      document.body.classList.toggle("dark-mode");
      const isDark = document.body.classList.contains("dark-mode");
      localStorage.setItem("darkMode", isDark);

      // Update icon
      const icon = this.querySelector("i");
      icon.className = isDark ? "fas fa-sun" : "fas fa-moon";
    });
  }

  // Restore theme
  const darkMode = localStorage.getItem("darkMode") === "true";
  if (darkMode) {
    document.body.classList.add("dark-mode");
    const icon = themeToggle.querySelector("i");
    if (icon) icon.className = "fas fa-sun";
  }

  // Modal Handlers
  setupModalHandlers();

  // Close sidebar on outside click (mobile)
  document.addEventListener("click", function (e) {
    if (window.innerWidth <= 1024) {
      if (!sidebar.contains(e.target) && !mobileMenuBtn.contains(e.target)) {
        sidebar.classList.remove("active");
      }
    }
  });

  // Cancel button handlers
  const cancelAssetBtn2 = document.getElementById("cancelAssetBtn2");
  if (cancelAssetBtn2) {
    cancelAssetBtn2.addEventListener("click", function () {
      const addAssetForm = document.getElementById("addAssetForm");
      if (addAssetForm) {
        addAssetForm.style.display = "none";
      }
    });
  }
}

function handleNavigation(page) {
  console.log("Navigating to:", page);

  // Hide all section cards except relevant ones based on page
  const wishlistSection = document.getElementById("wishlistSection");

  switch (page) {
    case "dashboard":
      // Show all dashboard content
      if (wishlistSection) wishlistSection.style.display = "none";
      break;
    case "wishlist":
      // Show wishlist section
      if (wishlistSection) wishlistSection.style.display = "block";
      break;
    case "portfolio":
    case "assets":
    case "market":
    case "analytics":
    case "reports":
      // You can add specific logic for each page here
      if (wishlistSection) wishlistSection.style.display = "none";
      break;
  }
}

function setupModalHandlers() {
  const modal = document.getElementById("portfolioModal");
  const closeModalBtn = document.getElementById("closeModal");
  const closeModalBtns = document.querySelectorAll(".close-modal");

  if (closeModalBtn) {
    closeModalBtn.addEventListener("click", function () {
      modal.classList.remove("active");
      modal.style.display = "none";
    });
  }

  closeModalBtns.forEach((btn) => {
    btn.addEventListener("click", function () {
      modal.classList.remove("active");
      modal.style.display = "none";
    });
  });

  // Close modal on outside click
  window.addEventListener("click", function (e) {
    if (e.target === modal) {
      modal.classList.remove("active");
      modal.style.display = "none";
    }
  });
}

// Update the create portfolio button to show modal correctly
const createPortfolioBtn = document.getElementById("createPortfolioBtn");
if (createPortfolioBtn) {
  createPortfolioBtn.addEventListener("click", function () {
    const modal = document.getElementById("portfolioModal");
    if (modal) {
      modal.classList.add("active");
      modal.style.display = "flex";
    }
  });
}

// Handle window resize
window.addEventListener("resize", function () {
  const sidebar = document.getElementById("sidebar");
  if (window.innerWidth > 1024) {
    sidebar.classList.remove("active");
  }
});

// Export for use in other scripts
window.dashboardUI = {
  showToast: function (message, type = "info") {
    const toast = document.getElementById("toast");
    if (toast) {
      toast.textContent = message;
      toast.className = "toast show";

      // Change color based on type
      if (type === "success") {
        toast.style.borderLeftColor = "var(--success)";
      } else if (type === "error") {
        toast.style.borderLeftColor = "var(--danger)";
      } else {
        toast.style.borderLeftColor = "var(--primary)";
      }

      setTimeout(() => {
        toast.classList.remove("show");
      }, 3000);
    }
  },

  showLoading: function () {
    const overlay = document.getElementById("loadingOverlay");
    if (overlay) {
      overlay.style.display = "flex";
    }
  },

  hideLoading: function () {
    const overlay = document.getElementById("loadingOverlay");
    if (overlay) {
      overlay.style.display = "none";
    }
  },
};
