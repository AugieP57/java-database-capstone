Section - MySQL Database Design

Tables:
  patients, 
  doctors, 
  appointments, 
  admin,
  clinic_locations
  payments

Tables structures:
  Example structure:
  ### Table: patients

  Example structure:
  ### Table: doctors
  
  Example structure:
  ### Table: appointments
	- id: INT, Primary Key, Auto Increment
	- doctor_id: INT, Foreign Key → doctors(id)
	- patient_id: INT, Foreign Key → patients(id)
	- appointment_time: DATETIME, Not Null
	- status: INT (0 = Scheduled, 1 = Completed, 2 = Cancelled)
 
 Example: admin
  ### Table: admin

Example structure:
  ### Table: clinic locations

Example structure:
  ### Table: payments

Section - MongoDB Collection Design

### Collection: prescriptions

```json
{
  "_id": "ObjectId('64abc123456')",
  "patientName": "John Smith",
  "appointmentId": 51,
  "medication": "Paracetamol",
  "dosage": "500mg",
  "doctorNotes": "Take 1 tablet every 6 hours.",
  "refillCount": 2,
  "pharmacy": {
    "name": "Walgreens SF",
    "location": "Market Street"
  }
}
