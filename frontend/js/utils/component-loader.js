// ============================================
// Component Loader
// Loads reusable HTML components
// ============================================

export async function loadComponents() {
  const components = [
    { id: "sidebar-container", path: "../components/sidebar.html" },
    { id: "navbar-container", path: "../components/navbar.html" },
    { id: "footer-container", path: "../components/footer.html" },
    { id: "modals-container", path: "../components/modals.html" },
    { id: "loading-overlay-container", path: "../components/loading.html" },
  ];

  const loadPromises = components.map(async ({ id, path }) => {
    const container = document.getElementById(id);
    if (container) {
      try {
        const response = await fetch(path);
        if (response.ok) {
          const html = await response.text();
          container.innerHTML = html;
        } else {
          console.warn(`Failed to load component: ${path}`);
        }
      } catch (error) {
        console.error(`Error loading component ${path}:`, error);
      }
    }
  });

  await Promise.all(loadPromises);
}

export function getComponentHTML(componentId) {
  const container = document.getElementById(`${componentId}-container`);
  return container ? container.innerHTML : "";
}
