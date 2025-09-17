// js/services/index.js
// NOTE: This file is an ES module (type="module")

// ── Imports ────────────────────────────────────────────────────────────────────
import { openModal } from '../components/modals.js';
import { API_BASE_URL } from '../config/config.js';
import { selectRole } from '../render.js'; // ensure render.js exports selectRole

// ── API Endpoints ──────────────────────────────────────────────────────────────
export const ADMIN_API  = `${API_BASE_URL}/admin`;
export const DOCTOR_API = `${API_BASE_URL}/doctor/login`;

// ── Helpers ───────────────────────────────────────────────────────────────────
async function postJSON(url, data) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return res;
}

// ── Global Handlers (attached to window) ──────────────────────────────────────
// Admin: expects inputs with IDs #adminUsername and #adminPassword
window.adminLoginHandler = async function adminLoginHandler(ev) {
  try {
    ev?.preventDefault?.();

    const usernameEl = document.getElementById('adminUsername');
    const passwordEl = document.getElementById('adminPassword');

    const username = usernameEl?.value?.trim() || '';
    const password = passwordEl?.value || '';

    if (!username || !password) {
      alert('Please enter both username and password.');
      return;
    }

    const admin = { username, password };

    const res = await postJSON(ADMIN_API, admin);

    if (!res.ok) {
      // e.g., 401/400
      alert('Invalid credentials!');
      return;
    }

    const data = await res.json();
    const token = data?.token;

    if (!token) {
      alert('Login failed: token not returned by server.');
      return;
    }

    // Store auth + role
    localStorage.setItem('authToken', token);
    selectRole?.('admin'); // persists role (render.js)

    // Optional: close modal or navigate as your app requires
    // closeModal('adminLogin'); // if you have a closeModal helper
    // location.href = '/dashboard.html';
  } catch (err) {
    console.error(err);
    alert('An unexpected error occurred. Please try again.');
  }
};

// Doctor: expects inputs with IDs #doctorEmail and #doctorPassword
window.doctorLoginHandler = async function doctorLoginHandler(ev) {
  try {
    ev?.preventDefault?.();

    const emailEl = document.getElementById('doctorEmail');
    const passwordEl = document.getElementById('doctorPassword');

    const email = emailEl?.value?.trim() || '';
    const password = passwordEl?.value || '';

    if (!email || !password) {
      alert('Please enter both email and password.');
      return;
    }

    const doctor = { email, password };

    const res = await postJSON(DOCTOR_API, doctor);

    if (!res.ok) {
      alert('Invalid credentials!');
      return;
    }

    const data = await res.json();
    const token = data?.token;

    if (!token) {
      alert('Login failed: token not returned by server.');
      return;
    }

    localStorage.setItem('authToken', token);
    selectRole?.('doctor'); // persists role (render.js)

    // Optional: close modal or navigate as your app requires
    // closeModal('doctorLogin');
    // location.href = '/doctor/dashboard.html';
  } catch (err) {
    console.error(err);
    alert('An unexpected error occurred. Please try again.');
  }
};

// ── Button Event Listeners ────────────────────────────────────────────────────
window.onload = function () {
  // Admin button opens Admin login modal
  const adminBtn = document.getElementById('adminLogin');
  if (adminBtn) {
    adminBtn.addEventListener('click', () => {
      openModal('adminLogin');
    });
  }

  // Doctor button opens Doctor login modal
  const doctorBtn = document.getElementById('doctorLogin');
  if (doctorBtn) {
    doctorBtn.addEventListener('click', () => {
      openModal('doctorLogin');
    });
  }

  // (Optional) If you use form submit buttons inside the modals:
  // document.getElementById('adminLoginForm')?.addEventListener('submit', adminLoginHandler);
  // document.getElementById('doctorLoginForm')?.addEventListener('submit', doctorLoginHandler);
};
