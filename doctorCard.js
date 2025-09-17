/* doctorCard.js — builds a doctor card DOM node */

// Optional imports if your bundler supports them. If not, these can be
// provided on window (e.g., window.getPatientData, window.showBookingOverlay, window.deleteDoctorById).
// import { getPatientData } from "../js/services/patientServices.js";
// import { deleteDoctorById } from "../js/services/doctorServices.js";
// import { showBookingOverlay } from "../js/components/modals.js";

/**
 * Create a doctor card element with role-aware actions.
 * @param {Object} doctor
 * @param {string} doctor.id
 * @param {string} doctor.name
 * @param {string} [doctor.specialty]
 * @param {string} [doctor.specialization]
 * @param {string} [doctor.email]
 * @param {string[]} [doctor.availability] - e.g., ["Mon AM", "Tue PM"]
 * @returns {HTMLDivElement}
 */
export function createDoctorCard(doctor) {
  // Main card container
  const card = document.createElement("div");
  card.classList.add("doctor-card");
  card.dataset.id = doctor?.id ?? "";

  // Current role
  const role = localStorage.getItem("userRole");

  // ----- Doctor info section -----
  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const name = document.createElement("h3");
  name.textContent = doctor?.name || "Unknown Doctor";

  const specialization = document.createElement("p");
  specialization.classList.add("doctor-specialty");
  specialization.textContent = (doctor?.specialty || doctor?.specialization || "").toString();

  const email = document.createElement("p");
  email.classList.add("doctor-email");
  email.textContent = doctor?.email || "";

  const availability = document.createElement("p");
  availability.classList.add("doctor-availability");
  const availList = Array.isArray(doctor?.availability) ? doctor.availability : [];
  availability.textContent = availList.length ? availList.join(", ") : "";

  infoDiv.appendChild(name);
  if (specialization.textContent) infoDiv.appendChild(specialization);
  if (email.textContent) infoDiv.appendChild(email);
  if (availability.textContent) infoDiv.appendChild(availability);

  // ----- Actions (role-based) -----
  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.className = "btn remove-btn";
    removeBtn.textContent = "Delete";

    removeBtn.addEventListener("click", async () => {
      const ok = confirm(`Delete ${doctor?.name ?? "this doctor"}?`);
      if (!ok) return;
      const token = localStorage.getItem("token");
      try {
        const fn = (window.deleteDoctorById || (async () => ({ ok: false })));
        const res = await fn(doctor?.id, token);
        if (res?.ok) {
          card.remove();
        } else {
          alert(res?.error || "Failed to delete doctor.");
        }
      } catch (err) {
        console.error(err);
        alert("An error occurred while deleting the doctor.");
      }
    });

    actionsDiv.appendChild(removeBtn);
  } else if (role === "patient") {
    const bookNow = document.createElement("button");
    bookNow.type = "button";
    bookNow.className = "btn prescription-btn";
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", () => {
      alert("Patient needs to login first.");
    });
    actionsDiv.appendChild(bookNow);
  } else if (role === "loggedPatient") {
    const bookNow = document.createElement("button");
    bookNow.type = "button";
    bookNow.className = "btn prescription-btn";
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", async (e) => {
      createRipple(e);
      const token = localStorage.getItem("token");
      try {
        const getPD = (window.getPatientData || (async () => null));
        const patientData = await getPD(token);
        const show = (window.showBookingOverlay || ((evt, d, p) => console.warn("showBookingOverlay not found", { d, p })));
        show(e, doctor, patientData);
      } catch (err) {
        console.error(err);
        alert("Unable to start booking. Please try again.");
      }
    });
    actionsDiv.appendChild(bookNow);
  } else {
    // Fallback: treat unknown roles like public patient
    const bookNow = document.createElement("button");
    bookNow.type = "button";
    bookNow.className = "btn prescription-btn";
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", () => alert("Please sign in to book."));
    actionsDiv.appendChild(bookNow);
  }

  // Compose card
  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);

  return card;
}

/** Simple click ripple that leverages .ripple CSS (patientDashboard.css) */
function createRipple(event) {
  try {
    const x = event.clientX;
    const y = event.clientY;
    const span = document.createElement("span");
    span.className = "ripple"; // styled in patientDashboard.css
    span.style.setProperty("--x", `${x}px`);
    span.style.setProperty("--y", `${y}px`);
    document.body.appendChild(span);
    span.addEventListener("animationend", () => span.remove(), { once: true });
  } catch (_) {}
}
