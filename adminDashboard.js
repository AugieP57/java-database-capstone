
// adminDashboard.js
// Dashboard logic for Admin to manage doctors
// Adjust import paths if your project structure differs.

import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

// -----------------------------
// DOM Elements (lazy getters)
// -----------------------------
const els = {
  get content() {
    return document.getElementById("content");
  },
  get searchBar() {
    return document.getElementById("searchBar");
  },
  get filterTime() {
    return document.getElementById("filterTime");
  },
  get filterSpecialty() {
    return document.getElementById("filterSpecialty");
  },
  get addDocBtn() {
    return document.getElementById("addDocBtn");
  },
  get addDoctorForm() {
    return document.getElementById("addDoctorForm");
  },
  get statusBar() {
    return document.getElementById("statusBar"); // optional helper area for messages
  },
};

// -----------------------------
// Init / Event Binding
// -----------------------------
(function initAdminDashboard() {
  // Add Doctor button opens the modal
  if (els.addDocBtn) {
    els.addDocBtn.addEventListener("click", () => openModal("addDoctor"));
  }

  // Search & Filters
  if (els.searchBar) {
    els.searchBar.addEventListener("input", debounce(filterDoctorsOnChange, 250));
  }
  if (els.filterTime) {
    els.filterTime.addEventListener("change", filterDoctorsOnChange);
  }
  if (els.filterSpecialty) {
    els.filterSpecialty.addEventListener("change", filterDoctorsOnChange);
  }

  // Add Doctor form submit handler (inside modal)
  if (els.addDoctorForm) {
    els.addDoctorForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      await adminAddDoctor();
    });
  }

  // Load doctors on page load
  window.addEventListener("DOMContentLoaded", () => {
    loadDoctorCards();
  });
})();

// -----------------------------
// Core: Load & Render Doctors
// -----------------------------
export async function loadDoctorCards() {
  try {
    setStatus("Loading doctors…");
    const doctors = await getDoctors();
    renderDoctorCards(doctors || []);
    clearStatus();
  } catch (err) {
    console.error("Failed to load doctors", err);
    setStatus("Failed to load doctors.", true);
  }
}

export function renderDoctorCards(doctors) {
  const contentDiv = els.content;
  if (!contentDiv) return;

  // Clear existing
  contentDiv.innerHTML = "";

  if (!Array.isArray(doctors) || doctors.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.textContent = "No doctors found";
    contentDiv.appendChild(empty);
    return;
  }

  // Render each doctor card
  for (const doc of doctors) {
    try {
      const card = createDoctorCard(doc);
      contentDiv.appendChild(card);
    } catch (err) {
      console.warn("Could not render doctor card", err, doc);
    }
  }
}

// -----------------------------
// Search / Filter Handling
// -----------------------------
async function filterDoctorsOnChange() {
  try {
    const search = (els.searchBar?.value || "").trim();
    const time = els.filterTime?.value || "";
    const specialty = els.filterSpecialty?.value || "";

    const filters = { search, time, specialty };
    setStatus("Filtering…");
    const doctors = await filterDoctors(filters);
    renderDoctorCards(doctors || []);
    clearStatus();
  } catch (err) {
    console.error("Filter failed", err);
    setStatus("Unable to filter doctors.", true);
  }
}

// -----------------------------
// Add Doctor Flow
// -----------------------------
async function adminAddDoctor() {
  try {
    const token = getAdminToken();
    if (!token) {
      alert("Missing or invalid admin session. Please log in again.");
      return;
    }

    // Collect form inputs (ensure your modal form uses these IDs/names)
    const name = getInputValue(["docName", "doctorName", "name"]);
    const specialty = getInputValue(["docSpecialty", "doctorSpecialty", "specialty"]);
    const email = getInputValue(["docEmail", "doctorEmail", "email"], "email");
    const password = getInputValue(["docPassword", "doctorPassword", "password"], "password");
    const mobile = getInputValue(["docMobile", "doctorMobile", "mobile", "phone"]);

    const availability = collectAvailability();

    const payload = {
      name,
      specialty,
      email,
      password,
      mobile,
      availability,
    };

    setStatus("Saving doctor…");
    const res = await saveDoctor(payload, token);

    if (res && (res.success || res.ok)) {
      toast("Doctor added successfully.");
      closeAddDoctorModal();
      await loadDoctorCards();
      clearStatus();
    } else {
      const msg = (res && (res.message || res.error)) || "Unable to add doctor.";
      alert(msg);
      clearStatus();
    }
  } catch (err) {
    console.error("Add doctor failed", err);
    alert("Failed to add doctor. Please try again.");
    clearStatus();
  }
}

// -----------------------------
// Helpers
// -----------------------------
function getAdminToken() {
  // Prefer a specific admin token, fall back to generic auth token
  return (
    localStorage.getItem("adminToken") ||
    localStorage.getItem("authToken") ||
    null
  );
}

function getInputValue(possibleIds, type = "text") {
  for (const id of possibleIds) {
    const el = document.getElementById(id);
    if (el && (el.type === type || !el.type)) {
      return (el.value || "").trim();
    }
  }
  return "";
}

function collectAvailability() {
  // Collect checkbox values. Expect either name="availability" or class="availability-checkbox".
  const selected = [];
  const byName = document.querySelectorAll('input[name="availability"]:checked');
  byName.forEach((el) => selected.push(el.value));
  if (selected.length > 0) return selected;

  const byClass = document.querySelectorAll(".availability-checkbox:checked");
  byClass.forEach((el) => selected.push(el.value));
  return selected;
}

function closeAddDoctorModal() {
  // If your modal system exposes closeModal, use it; otherwise, hide common IDs gracefully.
  try {
    if (typeof window.closeModal === "function") {
      window.closeModal("addDoctor");
      return;
    }
  } catch {}
  const modalEl =
    document.getElementById("addDoctorModal") ||
    document.getElementById("modal-addDoctor") ||
    document.querySelector('[data-modal="addDoctor"]');
  if (modalEl) {
    modalEl.classList.add("hidden");
    modalEl.setAttribute("aria-hidden", "true");
  }
}

function setStatus(msg, isError = false) {
  const bar = els.statusBar;
  if (bar) {
    bar.textContent = msg;
    bar.dataset.state = isError ? "error" : "info";
  }
}

function clearStatus() {
  const bar = els.statusBar;
  if (bar) {
    bar.textContent = "";
    bar.dataset.state = "";
  }
}

function toast(msg) {
  // Replace with your UI toast system if available
  try {
    if (window?.showToast) return window.showToast(msg);
  } catch {}
  // Fallback
  alert(msg);
}

function debounce(fn, wait = 250) {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn.apply(null, args), wait);
  };
}
