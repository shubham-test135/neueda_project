// ============================================
// Reusable Asset/Wishlist Modal Component
// ============================================

/**
 * Creates and shows a reusable asset/wishlist modal
 * @param {Object} options - Configuration options
 * @param {string} options.title - Modal title
 * @param {string} options.submitText - Submit button text
 * @param {Function} options.onSubmit - Callback function on submit
 * @param {Object} options.prefillData - Optional data to prefill form
 * @param {boolean} options.showQuantity - Show quantity field (default: false for wishlist)
 * @param {boolean} options.showPurchaseFields - Show purchase price/date (default: false for wishlist)
 */
export function showAssetModal(options = {}) {
  const {
    title = "Add Asset",
    submitText = "Add",
    onSubmit,
    prefillData = {},
    showQuantity = false,
    showPurchaseFields = false,
  } = options;

  // Remove existing modal if any
  const existingModal = document.getElementById("reusableAssetModal");
  if (existingModal) {
    existingModal.remove();
  }

  // Create modal
  const modal = createAssetModalElement({
    title,
    submitText,
    showQuantity,
    showPurchaseFields,
    prefillData,
  });

  document.body.appendChild(modal);

  // Setup event listeners
  const closeBtn = modal.querySelector(".modal-close");
  const cancelBtn = modal.querySelector("#cancelAssetBtn");
  const form = modal.querySelector("#reusableAssetForm");

  closeBtn?.addEventListener("click", () => closeModal(modal));
  cancelBtn?.addEventListener("click", () => closeModal(modal));

  modal.addEventListener("click", (e) => {
    if (e.target === modal) closeModal(modal);
  });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const formData = getFormData(form);

    if (onSubmit) {
      try {
        await onSubmit(formData);
        closeModal(modal);
      } catch (error) {
        console.error("Error submitting form:", error);
        // Keep modal open on error so user can retry
      }
    }
  });

  // Show modal - use both methods for compatibility
  try {
    if (typeof modal.showModal === "function") {
      modal.showModal();
    } else {
      // Fallback for older browsers
      modal.style.display = "flex";
      modal.setAttribute("open", "");
    }
  } catch (error) {
    console.error("Error showing modal:", error);
    modal.style.display = "flex";
    modal.setAttribute("open", "");
  }
}

/**
 * Creates the modal DOM element
 */
function createAssetModalElement(options) {
  const { title, submitText, showQuantity, showPurchaseFields, prefillData } =
    options;

  const modal = document.createElement("dialog");
  modal.id = "reusableAssetModal";
  modal.className = "modal";
  modal.setAttribute("aria-labelledby", "reusable-modal-title");

  modal.innerHTML = `
    <div class="modal-content">
      <div class="modal-header">
        <h2 id="reusable-modal-title">${title}</h2>
        <button class="modal-close" aria-label="Close modal" type="button">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="modal-body">
        <form id="reusableAssetForm">
          <div class="form-group">
            <label for="assetSymbol">Symbol / Ticker *</label>
            <input
              type="text"
              id="assetSymbol"
              name="symbol"
              class="input-field"
              placeholder="e.g., AAPL, GOOGL, MSFT"
              value="${prefillData.symbol || ""}"
              required
              aria-required="true"
            />
          </div>

          <div class="form-group">
            <label for="assetName">Name</label>
            <input
              type="text"
              id="assetName"
              name="name"
              class="input-field"
              placeholder="Asset name (optional)"
              value="${prefillData.name || ""}"
            />
          </div>

          <div class="form-group">
            <label for="assetCategory">Category *</label>
            <select
              id="assetCategory"
              name="category"
              class="select-input"
              required
              aria-required="true"
            >
              <option value="">Select category</option>
              <option value="STOCK" ${prefillData.category === "STOCK" ? "selected" : ""}>Stocks</option>
              <option value="BOND" ${prefillData.category === "BOND" ? "selected" : ""}>Bonds</option>
              <option value="CRYPTO" ${prefillData.category === "CRYPTO" ? "selected" : ""}>Cryptocurrency</option>
              <option value="ETF" ${prefillData.category === "ETF" ? "selected" : ""}>ETFs</option>
              <option value="MUTUAL_FUND" ${prefillData.category === "MUTUAL_FUND" ? "selected" : ""}>Mutual Funds</option>
            </select>
          </div>

          ${
            showQuantity
              ? `
          <div class="form-group">
            <label for="assetQuantity">Quantity *</label>
            <input
              type="number"
              id="assetQuantity"
              name="quantity"
              class="input-field"
              placeholder="Number of units"
              value="${prefillData.quantity || ""}"
              step="0.01"
              min="0"
              required
              aria-required="true"
            />
          </div>
          `
              : ""
          }

          ${
            showPurchaseFields
              ? `
          <div class="form-group">
            <label for="assetPurchasePrice">Purchase Price *</label>
            <input
              type="number"
              id="assetPurchasePrice"
              name="purchasePrice"
              class="input-field"
              placeholder="Price per unit"
              value="${prefillData.purchasePrice || ""}"
              step="0.01"
              min="0"
              required
              aria-required="true"
            />
          </div>

          <div class="form-group">
            <label for="assetPurchaseDate">Purchase Date</label>
            <input
              type="date"
              id="assetPurchaseDate"
              name="purchaseDate"
              class="input-field"
              value="${prefillData.purchaseDate || ""}"
            />
          </div>
          `
              : ""
          }

          <div class="form-group">
            <label for="assetTargetPrice">Target Price (Optional)</label>
            <input
              type="number"
              id="assetTargetPrice"
              name="targetPrice"
              class="input-field"
              placeholder="Set target price for alerts"
              value="${prefillData.targetPrice || ""}"
              step="0.01"
              min="0"
            />
          </div>

          <div class="form-group">
            <label for="assetNotes">Notes (Optional)</label>
            <textarea
              id="assetNotes"
              name="notes"
              class="textarea-field"
              rows="3"
              placeholder="Add your investment thesis or notes..."
            >${prefillData.notes || ""}</textarea>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn-secondary" id="cancelAssetBtn">
          Cancel
        </button>
        <button type="submit" form="reusableAssetForm" class="btn-primary">
          <i class="fas fa-${showPurchaseFields ? "plus" : "star"}"></i> ${submitText}
        </button>
      </div>
    </div>
  `;

  return modal;
}

/**
 * Closes and removes the modal
 */
function closeModal(modal) {
  modal.close();
  setTimeout(() => modal.remove(), 300); // Allow animation to complete
}

/**
 * Extracts form data
 */
function getFormData(form) {
  const formData = new FormData(form);
  const data = {};

  for (let [key, value] of formData.entries()) {
    if (value !== "") {
      data[key] = value;
    }
  }

  // Convert numeric fields
  if (data.quantity) data.quantity = parseFloat(data.quantity);
  if (data.purchasePrice) data.purchasePrice = parseFloat(data.purchasePrice);
  if (data.targetPrice) data.targetPrice = parseFloat(data.targetPrice);

  return data;
}
