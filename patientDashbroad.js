// patientDashboard.js
// Patient-facing dashboard: browse & filter doctors, login/signup modals
// Adjust import paths to your project structure.

import { createDoctorCard } from "./components/doctorCard.js";
import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors } from "./services/doctorServices.js";
import { patientLogin, patientSignup } from "./services/patientServices.js";

// ------------------------------------
// Lazy DOM getters
// ------------------------------------
const els = {
  get content() { return document.getElementById("content"); },
  get searchBar() { return document.getElementById("searchBar"); },
  get filterTime() { return document.getElementById("filterTime"); },
  get filterSpecialty() { return document.getElementById("filterSpecialty"); },
  get signupBtn() { return document.getElementById("patientSignup"); },
  get loginBtn() { return document.getElementById("patientLogin"); },
  get statusBar() { return document.getElementById("statusBar"); },
};

// ------------------------------------
// Page bootstrapping
// ------------------------------------
// Load initial doctor cards
document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();
});

// Bind modal triggers
document.addEventListener("DOMContentLoaded", () => {
  const btn = els.signupBtn;
  if (btn) btn.addEventListener("click", () => openModal("patientSignup"));
});

document.addEventListener("DOMContentLoaded", () => {
  const loginBtn = els.loginBtn;
  if (loginBtn) loginBtn.addEventListener("click", () => openModal("patientLogin"));
});

// Bind search & filters
document.addEventListener("DOMContentLoaded", () => {
  if (els.searchBar) els.searchBar.addEventListener("input", debounce(filterDoctorsOnChange, 250));
  if (els.filterTime) els.filterTime.addEventListener("change", filterDoctorsOnChange);
  if (els.filterSpecialty) els.filterSpecialty.addEventListener("change", filterDoctorsOnChange);
});

// Expose auth handlers to window for form onsubmit hooks
window.signupPatient = async function signupPatient() {
  try {
    const name = val(["signupName", "patientName", "name"]);
    const email = val(["signupEmail", "patientEmail", "email"], "email");
    const password = val(["signupPassword", "patientPassword", "password"], "password");
    const phone = val(["signupPhone", "patientPhone", "phone", "mobile"]);
    const address = val(["signupAddress", "patientAddress", "address"]);

    setStatus("Creating your account…");
    const res = await patientSignup({ name, email, password, phone, address });

    if (res && (res.success || res.ok)) {
      alert(res.message || "Signup successful. You can now log in.");
      closeModalSafe("patientSignup");
      await loadDoctorCards();
    } else {
      const msg = (res && (res.message || res.error)) || "Signup failed. Please try again.";
      alert(msg);
    }
  } catch (err) {
    console.error("signupPatient error", err);
    alert("Unable to sign up right now. Please try again later.");
  } finally {
    clearStatus();
  }
};

window.loginPatient = async function loginPatient() {
  try {
    const email = val(["loginEmail", "patientLoginEmail", "email"], "email");
    const password = val(["loginPassword", "patientLoginPassword", "password"], "password");

    setStatus("Signing you in…");
    const res = await patientLogin({ email, password });

    if (res && (res.success || res.ok)) {
      const token = res.token || (res.data && res.data.token);
      if (token) localStorage.setItem("patientToken", token);
      closeModalSafe("patientLogin");
      // Redirect to logged-in patient dashboard
      window.location.href = "loggedPatientDashboard.html";
    } else {
      const msg = (res && (res.message || res.error)) || "Invalid credentials. Please try again.";
      alert(msg);
    }
  } catch (err) {
    console.error("loginPatient error", err);
    alert("Unable to log in right now. Please try again later.");
  } finally {
    clearStatus();
  }
};

// ------------------------------------
// Core rendering
// ------------------------------------
export async function loadDoctorCards() {
  try {
    setStatus("Loading doctors…");
    const doctors = await getDoctors();
    renderDoctorCards(doctors || []);
  } catch (err) {
    console.error("Failed to load doctors", err);
    renderFallback("We couldn't load doctors right now. Please try again later.");
  } finally {
    clearStatus();
  }
}

export function renderDoctorCards(doctors) {
  const content = els.content;
  if (!content) return;
  content.innerHTML = "";

  if (!Array.isArray(doctors) || doctors.length === 0) {
    renderFallback("No doctors found.");
    return;
  }

  for (const doc of doctors) {
    try {
      const card = createDoctorCard(doc);
      content.appendChild(card);
    } catch (e) {
      console.warn("Failed to render doctor", e, doc);
    }
  }
}

function renderFallback(message) {
  const content = els.content;
  if (!content) return;
  content.innerHTML = `<p class="empty-state">${message}</p>`;
}

// ------------------------------------
// Filters
// ------------------------------------
async function filterDoctorsOnChange() {
  const name = (els.searchBar?.value || "").trim();
  const time = els.filterTime?.value || "";
  const specialty = els.filterSpecialty?.value || "";

  try {
    setStatus("Filtering…");
    const doctors = await filterDoctors(name, time, specialty);
    const contentDiv = els.content;
    if (!contentDiv) return;

    if (Array.isArray(doctors) && doctors.length > 0) {
      renderDoctorCards(doctors);
    } else {
      contentDiv.innerHTML = `<p>No doctors found with the given filters.</p>`;
    }
  } catch (err) {
    console.error("Filter error", err);
    renderFallback("Could not apply filters at this time.");
  } finally {
    clearStatus();
  }
}

// ------------------------------------
// Helpers
// ------------------------------------
function val(possibleIds, type = "text") {
  for (const id of possibleIds) {
    const el = document.getElementById(id);
    if (el && (el.type === type || !el.type)) return (el.value || "").trim();
  }
  return "";
}

function closeModalSafe(id) {
  try {
    if (typeof window.closeModal === "function") return window.closeModal(id);
  } catch {}
  const modal = document.getElementById(id) || document.querySelector(`[data-modal="${id}"]`);
  if (modal) {
    modal.classList.add("hidden");
    modal.setAttribute("aria-hidden", "true");
  }
}

function setStatus(msg) {
  const bar = els.statusBar;
  if (bar) bar.textContent = msg;
}

function clearStatus() {
  const bar = els.statusBar;
  if (bar) bar.textContent = "";
}

function debounce(fn, wait = 250) {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn.apply(null, args), wait);
  };
}
