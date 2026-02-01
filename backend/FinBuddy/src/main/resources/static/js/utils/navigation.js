// ============================================
// Navigation Module
// Handles sidebar, active states, and routing
// ============================================

export function initNavigation() {
  // Wait for sidebar to load
  setTimeout(() => {
    setupSidebar();
    setupMobileMenu();
    setActiveNavItem();
  }, 100);
}

function setupSidebar() {
  const sidebar = document.getElementById("sidebar");
  const sidebarToggle = document.getElementById("sidebarToggle");

  if (sidebarToggle) {
    sidebarToggle.addEventListener("click", () => {
      sidebar.classList.toggle("collapsed");
      localStorage.setItem(
        "sidebarCollapsed",
        sidebar.classList.contains("collapsed"),
      );
    });
  }

  // Restore sidebar state
  const sidebarCollapsed = localStorage.getItem("sidebarCollapsed") === "true";
  if (sidebarCollapsed && window.innerWidth > 1024) {
    sidebar.classList.add("collapsed");
  }
}

function setupMobileMenu() {
  const sidebar = document.getElementById("sidebar");
  const mobileMenuBtn = document.getElementById("mobileMenuBtn");

  if (mobileMenuBtn) {
    mobileMenuBtn.addEventListener("click", () => {
      sidebar.classList.toggle("active");
    });
  }

  // Close sidebar on outside click (mobile)
  document.addEventListener("click", (e) => {
    if (window.innerWidth <= 1024 && sidebar) {
      if (!sidebar.contains(e.target) && !mobileMenuBtn?.contains(e.target)) {
        sidebar.classList.remove("active");
      }
    }
  });

  // Close mobile menu on link click
  const navLinks = document.querySelectorAll(".nav-link");
  navLinks.forEach((link) => {
    link.addEventListener("click", () => {
      if (window.innerWidth <= 1024) {
        sidebar.classList.remove("active");
      }
    });
  });
}

function setActiveNavItem() {
  const currentPage = getCurrentPage();
  const navItems = document.querySelectorAll(".nav-item");

  navItems.forEach((item) => {
    const page = item.getAttribute("data-page");
    if (page === currentPage) {
      item.classList.add("active");
    } else {
      item.classList.remove("active");
    }
  });
}

function getCurrentPage() {
  const path = window.location.pathname;
  const page = path.split("/").pop().replace(".html", "") || "dashboard";
  return page;
}

// Handle window resize
window.addEventListener("resize", () => {
  const sidebar = document.getElementById("sidebar");
  if (window.innerWidth > 1024 && sidebar) {
    sidebar.classList.remove("active");
  }
});

export { getCurrentPage };
