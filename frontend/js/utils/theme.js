// ============================================
// Theme Module
// Handles dark/light mode toggle
// ============================================

export function initTheme() {
  setTimeout(() => {
    setupThemeToggle();
    restoreTheme();
  }, 100);
}

function setupThemeToggle() {
  const themeToggle = document.getElementById("themeToggle");

  if (themeToggle) {
    themeToggle.addEventListener("click", () => {
      document.body.classList.toggle("dark-mode");
      const isDark = document.body.classList.contains("dark-mode");
      localStorage.setItem("darkMode", isDark);

      // Update icon
      const icon = themeToggle.querySelector("i");
      if (icon) {
        icon.className = isDark ? "fas fa-sun" : "fas fa-moon";
      }
    });
  }
}

function restoreTheme() {
  const darkMode = localStorage.getItem("darkMode") === "true";
  const themeToggle = document.getElementById("themeToggle");

  if (darkMode) {
    document.body.classList.add("dark-mode");
    const icon = themeToggle?.querySelector("i");
    if (icon) {
      icon.className = "fas fa-sun";
    }
  }
}

export function toggleTheme() {
  document.body.classList.toggle("dark-mode");
  const isDark = document.body.classList.contains("dark-mode");
  localStorage.setItem("darkMode", isDark);
  return isDark;
}

export function isDarkMode() {
  return document.body.classList.contains("dark-mode");
}
