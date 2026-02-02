import { portfolioAPI } from "./utils/api.js";

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

        if (saved && portfolios.some(p => p.id === saved)) {
            select.value = saved;
        } else if (portfolios.length > 0) {
            select.value = portfolios[0].id;
            localStorage.setItem("activePortfolioId", portfolios[0].id);
        }


        select.addEventListener("change", () => {
            const id = select.value;
            localStorage.setItem("activePortfolioId", id);
            window.dispatchEvent(
                new CustomEvent("portfolioChanged", { detail: { portfolioId: id } })
            );
        });

        // Fire initial event
        if (select.value) {
            window.dispatchEvent(
                new CustomEvent("portfolioChanged", {
                    detail: { portfolioId: select.value },
                })
            );
        }
    } catch (err) {
        console.error("Navbar initialization failed", err);
    }
}
