/* header.js — role-aware header renderer */

// ---- Configurable routes (adjust to your project structure) ----
const ROUTES = {
  home: "/",
  doctorHome: "/pages/doctorDashboard.html",
  patientHome: "/pages/patientDashboard.html",
  appointments: "/pages/appointments.html",
  login: "/pages/login.html",
  signup: "/pages/signup.html",
};

/** Render the role-based header if applicable. */
export function renderHeader() {
  // Do not render role header on hard homepage (root path or index.html)
  const path = window.location.pathname;
  if (path.endsWith("/") || path.endsWith("/index.html")) {
    localStorage.removeItem("userRole");
    localStorage.removeItem("token");
    return; // skip injecting header on the homepage
  }

  const headerDiv = document.getElementById("header");
  if (!headerDiv) return;

  // Get session state
  const role = localStorage.getItem("userRole");
  const token = localStorage.getItem("token");

  // If a privileged role is set but token is missing => invalid session
  if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
    localStorage.removeItem("userRole");
    alert("Session expired or invalid login. Please log in again.");
    window.location.href = ROUTES.home;
    return;
  }

  let headerContent = "";

  // Base shell
  const start = `
    <header class="header" role="banner">
      <a class="logo" href="${ROUTES.home}" aria-label="Home">🏥 MedPortal</a>
      <nav aria-label="Primary">
        <ul>
  `;
  const end = `
        </ul>
      </nav>
    </header>
  `;

  // Role-specific nav
  if (role === "admin") {
    headerContent = `
      ${start}
        <li><button id="addDocBtn" class="adminBtn" type="button">Add Doctor</button></li>
        <li><a id="logoutLink" href="#">Logout</a></li>
      ${end}
    `;
  } else if (role === "doctor") {
    headerContent = `
      ${start}
        <li><a id="homeBtn" href="${ROUTES.doctorHome}">Home</a></li>
        <li><a id="logoutLink" href="#">Logout</a></li>
      ${end}
    `;
  } else if (role === "loggedPatient") {
    headerContent = `
      ${start}
        <li><a id="homeBtn" href="${ROUTES.patientHome}">Home</a></li>
        <li><a id="appointmentsBtn" href="${ROUTES.appointments}">Appointments</a></li>
        <li><a id="logoutPatientLink" href="#">Logout</a></li>
      ${end}
    `;
  } else {
    // Default public/patient (not logged in)
    headerContent = `
      ${start}
        <li><a id="loginBtn" href="${ROUTES.login}">Login</a></li>
        <li><a id="signupBtn" href="${ROUTES.signup}">Sign Up</a></li>
      ${end}
    `;
    // Ensure we reflect this as a basic patient role if desired by app logic
    if (!role) localStorage.setItem("userRole", "patient");
  }

  headerDiv.innerHTML = headerContent;
  attachHeaderButtonListeners();
}

/** Attach event listeners to header controls if present. */
export function attachHeaderButtonListeners() {
  // Admin: Add Doctor opens modal
  const addDocBtn = document.getElementById("addDocBtn");
  if (addDocBtn) {
    addDocBtn.addEventListener("click", () => {
      if (typeof window.openModal === "function") {
        window.openModal("addDoctor");
      } else {
        // Fallback if modal system not yet loaded
        console.warn("openModal not found. Ensure modals.js is loaded.");
        alert("Add Doctor modal unavailable.");
      }
    });
  }

  // Logout for admin/doctor
  const logoutLink = document.getElementById("logoutLink");
  if (logoutLink) logoutLink.addEventListener("click", (e) => { e.preventDefault(); logout(); });

  // Logged patient logout
  const logoutPatientLink = document.getElementById("logoutPatientLink");
  if (logoutPatientLink) logoutPatientLink.addEventListener("click", (e) => { e.preventDefault(); logoutPatient(); });

  // Optional: ensure Home buttons route correctly via JS if needed
  const homeBtn = document.getElementById("homeBtn");
  if (homeBtn) {
    homeBtn.addEventListener("click", (e) => {
      // let anchors do their default navigation; this is here if you prefer JS-driven routing
    });
  }

  const appointmentsBtn = document.getElementById("appointmentsBtn");
  if (appointmentsBtn) {
    appointmentsBtn.addEventListener("click", (e) => {
      // default anchor behavior
    });
  }

  const loginBtn = document.getElementById("loginBtn");
  const signupBtn = document.getElementById("signupBtn");
  if (loginBtn) loginBtn.addEventListener("click", () => localStorage.setItem("userRole", "patient"));
  if (signupBtn) signupBtn.addEventListener("click", () => localStorage.setItem("userRole", "patient"));
}

/** Clear all session and return to landing page. */
export function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  window.location.href = ROUTES.home;
}

/** Logged patient logout: keep role as public patient and go to patient dashboard. */
export function logoutPatient() {
  localStorage.removeItem("token");
  localStorage.setItem("userRole", "patient");
  window.location.href = ROUTES.patientHome;
}

// Expose to window for inline handlers if needed
window.renderHeader = renderHeader;
window.logout = logout;
window.logoutPatient = logoutPatient;
window.attachHeaderButtonListeners = attachHeaderButtonListeners;

// Auto-render once DOM is ready
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", renderHeader);
} else {
  renderHeader();
}
