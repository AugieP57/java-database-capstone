// js/services/doctorServices.js

// ── Imports ────────────────────────────────────────────────────────────────────
import { API_BASE_URL } from "../config/config.js";

// ── Base Endpoint ──────────────────────────────────────────────────────────────
const DOCTOR_API = `${API_BASE_URL}/doctor`;

// ── Internal helper: safe fetch wrapper ────────────────────────────────────────
async function requestJSON(url, options = {}) {
  try {
    const res = await fetch(url, options);

    // Attempt to parse JSON even on non-2xx so we can surface server messages
    const payload = await res
      .json()
      .catch(() => ({ message: "Invalid JSON response." }));

    if (!res.ok) {
      const message =
        payload?.message ||
        payload?.error ||
        `Request failed with status ${res.status}`;
      return { success: false, message, data: null, status: res.status };
    }

    return { success: true, message: payload?.message || "OK", data: payload };
  } catch (err) {
    console.error("[doctorServices] Network/Unexpected error:", err);
    return {
      success: false,
      message: "Network error. Please check your connection and try again.",
      data: null,
      status: 0,
    };
  }
}

// ── Public API ────────────────────────────────────────────────────────────────

/**
 * Get all doctors
 * @returns {Promise<Array>} list of doctors (empty array on failure)
 */
export async function getDoctors() {
  const { success, data } = await requestJSON(DOCTOR_API, {
    method: "GET",
  });

  // Expecting server returns an array or { doctors: [...] }
  if (!success) return [];
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.doctors)) return data.doctors;

  return []; // fallback to non-breaking empty list
}

/**
 * Delete a doctor by id
 * @param {string} id - doctor unique id
 * @param {string} token - auth token (e.g., JWT)
 * @returns {Promise<{success:boolean, message:string}>}
 */
export async function deleteDoctor(id, token) {
  if (!id) {
    return { success: false, message: "Doctor id is required." };
  }
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const { success, message } = await requestJSON(`${DOCTOR_API}/${encodeURIComponent(id)}`, {
    method: "DELETE",
    headers,
  });

  return { success, message };
}

/**
 * Save (add) a new doctor
 * @param {object} doctor - doctor details (name, email, availability, etc.)
 * @param {string} token - admin auth token
 * @returns {Promise<{success:boolean, message:string, data?:any}>}
 */
export async function saveDoctor(doctor, token) {
  if (!doctor || typeof doctor !== "object") {
    return { success: false, message: "Doctor object is required." };
  }

  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const { success, message, data } = await requestJSON(DOCTOR_API, {
    method: "POST",
    headers,
    body: JSON.stringify(doctor),
  });

  return { success, message, data };
}

/**
 * Filter doctors by optional name, time, specialty
 * Uses route parameters; pass null/'' for unused slots.
 * Example: /doctor/filter/{name}/{time}/{specialty}
 * @param {string|null} name
 * @param {string|null} time
 * @param {string|null} specialty
 * @returns {Promise<Array>} filtered list (empty on none/error)
 */
export async function filterDoctors(name, time, specialty) {
  const seg = (v) =>
    (v === null || v === undefined || `${v}`.trim() === "") ? "null" : encodeURIComponent(`${v}`.trim());

  const url = `${DOCTOR_API}/filter/${seg(name)}/${seg(time)}/${seg(specialty)}`;

  const { success, data } = await requestJSON(url, { method: "GET" });
  if (!success) return [];

  // Accept either array or { doctors: [...] }
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.doctors)) return data.doctors;

  return [];
}

// ── Notes ─────────────────────────────────────────────────────────────────────
// • Keep token management (e.g., reading from localStorage) in UI/controller code.
// • This service layer focuses purely on communication with the backend.
// • If your backend uses different routes (e.g., POST /doctor/add), adjust the
//   endpoint paths above accordingly.
