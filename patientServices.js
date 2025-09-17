import { API_BASE_URL } from "../config/config.js";

const PATIENT_API = `${API_BASE_URL}/patient`;

// ── Internal helper: JSON request wrapper ──────────────────────────────────────
async function requestJSON(url, options = {}) {
  try {
    const res = await fetch(url, options);

    // Try to parse JSON regardless of status, so we can surface server messages
    const payload = await res.json().catch(() => ({}));

    if (!res.ok) {
      return {
        success: false,
        status: res.status,
        message:
          payload?.message || payload?.error || `Request failed with ${res.status}`,
        data: null,
      };
    }

    return {
      success: true,
      status: res.status,
      message: payload?.message || "OK",
      data: payload,
    };
  } catch (err) {
    console.error("[patientServices] Network/Unexpected error:", err);
    return {
      success: false,
      status: 0,
      message: "Network error. Please check your connection and try again.",
      data: null,
    };
  }
}

// ── Public API ────────────────────────────────────────────────────────────────

/**
 * Patient signup
 * @param {Object} data - { name, email, password, ... }
 * @returns {Promise<{success:boolean, message:string, data?:any}>}
 */
export async function patientSignup(data) {
  // POST /patient/signup
  const { success, message, data: payload } = await requestJSON(
    `${PATIENT_API}/signup`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    }
  );
  return { success, message, data: payload };
}

/**
 * Patient login
 * Returns the raw fetch Response so caller can inspect status, headers, token, etc.
 * NOTE: Remove any logging in production.
 * @param {Object} data - { email, password }
 * @returns {Promise<Response>} raw fetch Response
 */
export async function patientLogin(data) {
  // console.debug("[patientServices] patientLogin payload:", data); // Dev-only
  // POST /patient/login
  return fetch(`${PATIENT_API}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

/**
 * Get currently logged-in patient data
 * @param {string} token - auth token (e.g., JWT stored in localStorage)
 * @returns {Promise<Object|null>} patient object or null on failure
 */
export async function getPatientData(token) {
  if (!token) return null;

  const headers = { Authorization: `Bearer ${token}` };

  // GET /patient/me
  const { success, data } = await requestJSON(`${PATIENT_API}/me`, {
    method: "GET",
    headers,
  });

  if (!success) return null;

  // Accept either direct object or { patient: {...} }
  return data?.patient ?? data ?? null;
}

/**
 * Fetch appointments for a given patient (shared for patient/doctor dashboards)
 * Backend should route based on role path segment.
 * Example assumed route: /patient/{user}/{id}/appointments
 * @param {string} id - patient id
 * @param {string} token - auth token
 * @param {"patient"|"doctor"} user - who is requesting
 * @returns {Promise<Array|null>} appointments array, or null on failure
 */
export async function getPatientAppointments(id, token, user) {
  if (!id || !user) return null;

  const headers = { Authorization: `Bearer ${token}` };
  const url = `${PATIENT_API}/${encodeURIComponent(user)}/${encodeURIComponent(
    id
  )}/appointments`;

  const { success, data } = await requestJSON(url, { method: "GET", headers });

  if (!success) return null;

  // Accept either array or { appointments: [...] }
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.appointments)) return data.appointments;

  return [];
}

/**
 * Filter appointments by condition/name
 * Example assumed route: /patient/appointments/filter/{condition}/{name}
 * Pass null/'' to skip a filter; backend should interpret "null" as no filter.
 * @param {string|null} condition - e.g., "pending", "consulted"
 * @param {string|null} name - patient/doctor name (depends on role/backend)
 * @param {string} token - auth token
 * @returns {Promise<Array>} filtered list (empty array on failure)
 */
export async function filterAppointments(condition, name, token) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;

  const seg = (v) =>
    v === null || v === undefined || `${v}`.trim() === ""
      ? "null"
      : encodeURIComponent(`${v}`.trim());

  const url = `${PATIENT_API}/appointments/filter/${seg(condition)}/${seg(
    name
  )}`;

  const { success, data, message } = await requestJSON(url, {
    method: "GET",
    headers,
  });

  if (!success) {
    console.warn("[patientServices] filterAppointments failed:", message);
    // Optionally alert on unexpected errors in UI layer; service returns []
    return [];
  }

  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.appointments)) return data.appointments;

  return [];
}

// ── Notes ─────────────────────────────────────────────────────────────────────
// • Adjust endpoint paths if your backend differs (e.g., /api/patient vs /patient).
// • Keep UI concerns (alerts, toasts, navigation) out of this service file.
// • For consistency, most functions return { success, message, data? } or plain data.
// • patientLogin returns the raw Response per spec so the caller can extract tokens.
