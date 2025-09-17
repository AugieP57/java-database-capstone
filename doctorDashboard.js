// doctorDashboard.js
// Doctor dashboard: appointments list, search & date filters
// Adjust import paths to match your project layout.

import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

// --------------------------------------------------
// Globals
// --------------------------------------------------
let patientTableBody; // tbody element where rows are rendered (#patientTableBody)
let selectedDate; // ISO yyyy-mm-dd string representing the selected date
let token; // auth token for the doctor
let patientName = null; // search filter value ("null" string when empty)

// Utility: today in yyyy-mm-dd for input[type=date]
function todayISO() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

// Utility: safe get element
function $(id) {
  return document.getElementById(id);
}

// Utility: show single full-width row message in the table body
function setTableMessage(message) {
  if (!patientTableBody) return;
  patientTableBody.innerHTML = "";
  const tr = document.createElement("tr");
  const td = document.createElement("td");
  td.colSpan = 999; // span across all columns
  td.textContent = message;
  td.className = "table-message";
  tr.appendChild(td);
  patientTableBody.appendChild(tr);
}

// --------------------------------------------------
// Core: load appointments
// --------------------------------------------------
export async function loadAppointments() {
  try {
    if (!patientTableBody) patientTableBody = $("patientTableBody");

    // Clear existing content first for perceived responsiveness
    if (patientTableBody) patientTableBody.innerHTML = "";

    const dateParam = selectedDate || todayISO();
    const nameParam = (patientName && patientName.trim() !== "") ? patientName.trim() : "null";

    // Fetch
    const appts = await getAllAppointments(dateParam, nameParam, token);

    // Render
    if (!appts || !Array.isArray(appts) || appts.length === 0) {
      setTableMessage("No Appointments found for today");
      return;
    }

    for (const appt of appts) {
      try {
        // Extract patient details as needed by your row component
        // Example assumes appt has { patient, time, status, ... }
        // createPatientRow should handle the structure it expects.
        const row = createPatientRow(appt);
        if (row) patientTableBody.appendChild(row);
      } catch (rowErr) {
        console.warn("Failed to create row for appointment", rowErr, appt);
      }
    }
  } catch (err) {
    console.error("Error loading appointments:", err);
    setTableMessage("Unable to load appointments. Please try again later.");
  }
}

// --------------------------------------------------
// Event Bindings
// --------------------------------------------------
function bindSearch() {
  const searchEl = $("searchBar");
  if (!searchEl) return;
  searchEl.addEventListener("input", () => {
    patientName = searchEl.value && searchEl.value.trim() !== "" ? searchEl.value.trim() : "null";
    loadAppointments();
  });
}

function bindFilters() {
  const todayBtn = $("todayButton");
  const datePicker = $("datePicker");

  if (todayBtn) {
    todayBtn.addEventListener("click", () => {
      selectedDate = todayISO();
      if (datePicker) datePicker.value = selectedDate;
      loadAppointments();
    });
  }

  if (datePicker) {
    // Initialize the picker with today if empty
    if (!datePicker.value) datePicker.value = selectedDate || todayISO();

    datePicker.addEventListener("change", () => {
      selectedDate = datePicker.value || todayISO();
      loadAppointments();
    });
  }
}

// --------------------------------------------------
// Bootstrapping on page load
// --------------------------------------------------
window.addEventListener("DOMContentLoaded", async () => {
  // Init globals
  patientTableBody = $("patientTableBody");
  selectedDate = todayISO();
  token = localStorage.getItem("doctorToken") || localStorage.getItem("authToken") || null;
  patientName = null; // will be treated as "null" when querying if empty

  // Optional: if your app defines a content renderer, call it
  try {
    if (typeof window.renderContent === "function") {
      window.renderContent();
    }
  } catch {}

  bindSearch();
  bindFilters();

  // Initial load for today
  const datePicker = $("datePicker");
  if (datePicker) datePicker.value = selectedDate;

  await loadAppointments();
});
